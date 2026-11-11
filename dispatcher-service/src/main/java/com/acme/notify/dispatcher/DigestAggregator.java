package com.acme.notify.dispatcher;

import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.Priority;
import com.acme.notify.common.TopicNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Flushes due digest windows into a single summary notification such as "5 people liked your comment". */
@Component
@RequiredArgsConstructor
@Slf4j
public class DigestAggregator {

    public static final String DIGEST_SUFFIX = ".digest";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, Object> kafka;

    @Scheduled(fixedDelayString = "${notify.digest.poll-ms:5000}")
    @SchedulerLock(name = "digestAggregator", lockAtMostFor = "PT30S")
    public void flushDue() {
        var due = redis.opsForZSet().rangeByScore(DigestBuffer.DUE_KEY, 0, System.currentTimeMillis(), 0, 500);
        if (due == null) return;
        for (String stream : due) {
            try {
                flush(stream);
            } catch (Exception e) {
                log.error("Digest flush failed for {}", stream, e);
            }
        }
    }

    private void flush(String stream) {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().range(stream, Range.unbounded());
        if (records == null || records.isEmpty()) {
            redis.opsForZSet().remove(DigestBuffer.DUE_KEY, stream);
            return;
        }
        List<NotificationEvent> events = records.stream().map(this::parse).toList();
        NotificationEvent first = events.get(0);

        Map<String, Object> vars = new HashMap<>(first.variables() == null ? Map.of() : first.variables());
        vars.put("count", events.size());
        vars.put("actors", events.stream()
                .map(e -> e.variables() == null ? null : e.variables().get("actorName"))
                .filter(java.util.Objects::nonNull).map(Object::toString)
                .distinct().limit(3).toList());

        NotificationEvent summary = new NotificationEvent(UUID.randomUUID(), first.userId(),
                first.templateKey() + DIGEST_SUFFIX, Priority.MEDIUM, first.category(), first.channels(),
                first.locale(), vars, first.recipients(), null, Instant.now());

        kafka.send(TopicNames.REQ_MEDIUM, summary.userId(), summary);
        redis.delete(stream);
        redis.opsForZSet().remove(DigestBuffer.DUE_KEY, stream);
        log.info("Digest flushed: user={} template={} count={}", first.userId(), first.templateKey(), events.size());
    }

    private NotificationEvent parse(MapRecord<String, Object, Object> record) {
        try {
            return mapper.readValue((String) record.getValue().get("p"), NotificationEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
