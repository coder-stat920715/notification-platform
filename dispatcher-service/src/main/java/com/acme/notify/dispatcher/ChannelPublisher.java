package com.acme.notify.dispatcher;

import com.acme.notify.common.Channel;
import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.Priority;
import com.acme.notify.common.TopicNames;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publishToChannel(Channel channel, NotificationEvent event) {
        // BATCH shares the standard lane; HIGH has its own lane per channel.
        Priority lane = event.priority() == Priority.HIGH ? Priority.HIGH : Priority.MEDIUM;
        kafka.send(TopicNames.channel(channel, lane), event.userId(), event.forChannel(channel));
    }
}
