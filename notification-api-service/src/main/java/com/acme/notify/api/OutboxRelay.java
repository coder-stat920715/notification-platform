package com.acme.notify.api;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Transactional-outbox relay. At very high volume replace with Debezium CDC. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelayString = "${notify.outbox.poll-ms:50}")
    @Transactional
    public void relay() {
        var batch = outbox.lockNextBatch(500);
        for (OutboxEvent e : batch) {
            try {
                kafka.send(e.getTopic(), e.getPartitionKey(), e.getPayload()).get(5, TimeUnit.SECONDS);
                e.setPublished(true);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                log.warn("Outbox publish failed id={}, will retry: {}", e.getId(), ex.getMessage());
                break;      // preserve ordering, retry on the next tick
            }
        }
    }
}
