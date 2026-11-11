package com.acme.notify.tracker;

import com.acme.notify.common.StatusEvent;
import com.acme.notify.common.TopicNames;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusEventConsumer {

    private final StatusRecorder recorder;

    @KafkaListener(topics = TopicNames.STATUS, groupId = "analytics-tracker")
    public void on(StatusEvent event) {
        recorder.record(event);
    }
}
