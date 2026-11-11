package com.acme.notify.workers.handler;

import com.acme.notify.common.Channel;
import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes to connected WebSocket clients on /topic/users.{userId} and keeps the last 200 messages in a Redis
 * list as an inbox for offline users. For multi-pod fan-out, swap the simple broker for a STOMP relay.
 */
@Component
@RequiredArgsConstructor
public class InAppChannelHandler implements NotificationChannelHandler {

    private final SimpMessagingTemplate websocket;
    private final StringRedisTemplate redis;

    @Override public Channel channel() { return Channel.IN_APP; }

    @Override
    public DeliveryResult send(RenderedNotification n) {
        String inboxKey = "inbox:" + n.userId();
        redis.opsForList().leftPush(inboxKey, n.body());
        redis.opsForList().trim(inboxKey, 0, 199);
        websocket.convertAndSend("/topic/users." + n.userId(),
                Map.of("id", n.notificationId(), "title", n.subject(), "body", n.body()));
        return DeliveryResult.sent("websocket", n.notificationId());
    }
}
