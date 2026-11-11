package com.acme.notify.api;

import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.NotificationStatusView;
import com.acme.notify.common.Priority;
import com.acme.notify.common.TopicNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NotificationApiService {

    private static final String IDEM_PREFIX = "notif:idem:api:";

    private final NotificationRequestRepository requests;
    private final OutboxRepository outbox;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final StatusClient statusClient;

    @Transactional
    public SendNotificationResponse accept(String idempotencyKey, SendNotificationRequest r) {
        // Fast path: Redis NX. Durable path: unique constraint on idempotency_key.
        Boolean first = redis.opsForValue().setIfAbsent(IDEM_PREFIX + idempotencyKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(first)) {
            return requests.findByIdempotencyKey(idempotencyKey)
                    .map(e -> new SendNotificationResponse(e.getId(), e.getStatus()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "A request with this Idempotency-Key is still being accepted"));
        }
        var existing = requests.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new SendNotificationResponse(existing.get().getId(), existing.get().getStatus());
        }

        NotificationRequest entity = NotificationRequest.from(UUID.randomUUID(), idempotencyKey, r);
        requests.save(entity);

        NotificationEvent event = new NotificationEvent(entity.getId(), r.userId(), r.templateKey(),
                r.priority(), r.category(), r.channels(), entity.getLocale(),
                r.variables() == null ? Map.of() : r.variables(), Map.of(), r.scheduledAt(), Instant.now());

        // Same DB transaction as the request row: no dual-write loss between DB and Kafka.
        outbox.save(OutboxEvent.of(entity.getId().toString(), r.userId(), topicFor(r.priority()), toJson(event)));
        return new SendNotificationResponse(entity.getId(), DeliveryStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public NotificationStatusView status(UUID id) {
        NotificationRequest req = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown notification"));
        NotificationStatusView tracked = statusClient.fetch(id);
        if (tracked != null && !tracked.perChannel().isEmpty()) return tracked;
        Map<com.acme.notify.common.Channel, DeliveryStatus> pending = new java.util.EnumMap<>(com.acme.notify.common.Channel.class);
        req.getChannels().forEach(c -> pending.put(c, DeliveryStatus.PENDING));
        return new NotificationStatusView(id, pending, java.util.List.of());
    }

    private String topicFor(Priority p) {
        return switch (p) {
            case HIGH -> TopicNames.REQ_HIGH;
            case MEDIUM -> TopicNames.REQ_MEDIUM;
            case BATCH -> TopicNames.REQ_BATCH;
        };
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
