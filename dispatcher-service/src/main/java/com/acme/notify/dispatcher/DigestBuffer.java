package com.acme.notify.dispatcher;

import com.acme.notify.common.Channel;
import com.acme.notify.common.NotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Buffers digestible events in a Redis Stream per user/channel/template until the window closes. */
@Component
@RequiredArgsConstructor
public class DigestBuffer {

    public static final String DUE_KEY = "digest:due";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    @Value("${notify.digest.window:PT15M}")
    private Duration window;

    public void add(NotificationEvent event, Channel channel) {
        String stream = "digest:%s:%s:%s".formatted(event.userId(), channel, event.templateKey());
        redis.opsForStream().add(StreamRecords.string(Map.of("p", toJson(event.forChannel(channel)))).withStreamKey(stream));
        redis.expire(stream, Duration.ofHours(24));
        // Only the first event of a window sets the due time, so the window is fixed, not sliding.
        redis.opsForZSet().addIfAbsent(DUE_KEY, stream, Instant.now().plus(window).toEpochMilli());
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
