package com.acme.notify.dispatcher;

import com.acme.notify.common.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Holds events for quiet hours or future scheduling in a Redis ZSET (score = due epoch millis). */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeferredScheduler {

    private static final String KEY = "notif:deferred";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ChannelPublisher publisher;

    /** The event must already be narrowed to one channel (see NotificationEvent.forChannel). */
    public void deferUntil(NotificationEvent event, Instant when) {
        try {
            redis.opsForZSet().add(KEY, mapper.writeValueAsString(event), when.toEpochMilli());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Scheduled(fixedDelayString = "${notify.deferred.poll-ms:1000}")
    @SchedulerLock(name = "deferredDrain", lockAtMostFor = "PT30S")
    public void drain() {
        var ready = redis.opsForZSet().rangeByScore(KEY, 0, System.currentTimeMillis(), 0, 1000);
        if (ready == null) return;
        for (String json : ready) {
            Long removed = redis.opsForZSet().remove(KEY, json);      // claim: only one pod wins
            if (removed == null || removed == 0) continue;
            try {
                NotificationEvent event = mapper.readValue(json, NotificationEvent.class);
                publisher.publishToChannel(event.channels().iterator().next(), event);
            } catch (Exception e) {
                log.error("Failed to release deferred event, re-queueing: {}", e.getMessage());
                redis.opsForZSet().add(KEY, json, System.currentTimeMillis() + 30_000);
            }
        }
    }
}
