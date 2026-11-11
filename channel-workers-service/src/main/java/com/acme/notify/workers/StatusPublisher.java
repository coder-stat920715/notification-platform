package com.acme.notify.workers;

import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.StatusEvent;
import com.acme.notify.common.TopicNames;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publish(NotificationEvent event, Channel channel, DeliveryStatus status,
                        String provider, String providerMessageId, String error) {
        kafka.send(TopicNames.STATUS, event.notificationId().toString(),
                new StatusEvent(event.notificationId(), event.userId(), channel, status,
                        provider, providerMessageId, 1, error, Instant.now()));
    }
}
