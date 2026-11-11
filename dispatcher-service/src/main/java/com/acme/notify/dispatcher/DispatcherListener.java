package com.acme.notify.dispatcher;

import com.acme.notify.common.Channel;
import com.acme.notify.common.ContactView;
import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.PreferenceView;
import com.acme.notify.common.StatusEvent;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.TopicNames;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Order of checks: opt-out, scheduling, quiet hours (defer, never drop), frequency cap,
 * digest decision, then route to the channel topic. Critical categories skip everything except routing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DispatcherListener {

    private final PreferenceClient prefClient;
    private final ChannelResolver resolver;
    private final SlidingWindowRateLimiter limiter;
    private final DigestBuffer digestBuffer;
    private final DeferredScheduler scheduler;
    private final ChannelPublisher publisher;
    private final KafkaTemplate<String, Object> kafka;

    @KafkaListener(topics = TopicNames.REQ_HIGH, groupId = "dispatcher-high", containerFactory = "highPriorityFactory")
    public void high(NotificationEvent event) { handle(event); }

    @KafkaListener(topics = TopicNames.REQ_MEDIUM, groupId = "dispatcher-medium", containerFactory = "standardFactory")
    public void medium(NotificationEvent event) { handle(event); }

    @KafkaListener(topics = TopicNames.REQ_BATCH, groupId = "dispatcher-batch", containerFactory = "standardFactory")
    public void batch(NotificationEvent event) { handle(event); }

    void handle(NotificationEvent event) {
        PreferenceView prefs = prefClient.preferences(event.userId());
        ContactView contact = prefClient.contact(event.userId());
        Map<Channel, String> recipients = contact.asRecipients();
        NotificationEvent enriched = event.withRecipients(recipients);
        boolean critical = event.category().isCritical();
        Instant now = Instant.now();

        for (Channel ch : resolver.resolve(event, prefs, recipients)) {
            if (!critical && event.scheduledAt() != null && event.scheduledAt().isAfter(now)) {
                scheduler.deferUntil(enriched.forChannel(ch), event.scheduledAt());
                continue;
            }
            if (!critical && prefs.inQuietHours(ch, event.category(), now)) {
                scheduler.deferUntil(enriched.forChannel(ch), prefs.nextWindowStart(ch, event.category(), now));
                continue;
            }
            if (!critical && !limiter.tryAcquire(event.userId(), ch, event.category(),
                    prefs.capFor(ch, event.category()), Duration.ofHours(1))) {
                log.info("Rate limited: user={} channel={} category={}", event.userId(), ch, event.category());
                publishFailure(event, ch, "RATE_LIMITED");
                continue;
            }
            boolean alreadyDigest = event.templateKey().endsWith(DigestAggregator.DIGEST_SUFFIX);
            if (event.category().isDigestible() && !alreadyDigest) {
                digestBuffer.add(enriched, ch);
                continue;
            }
            publisher.publishToChannel(ch, enriched);
        }
    }

    private void publishFailure(NotificationEvent e, Channel ch, String reason) {
        kafka.send(TopicNames.STATUS, e.notificationId().toString(),
                new StatusEvent(e.notificationId(), e.userId(), ch, DeliveryStatus.FAILED,
                        "dispatcher", null, 0, reason, Instant.now()));
    }
}
