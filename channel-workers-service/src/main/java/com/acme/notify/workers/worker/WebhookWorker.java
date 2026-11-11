package com.acme.notify.workers.worker;

import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.TopicNames;
import com.acme.notify.workers.ChannelDeliveryService;
import com.acme.notify.workers.IdempotentNotification;
import com.acme.notify.workers.PermanentDeliveryException;
import com.acme.notify.workers.StatusPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookWorker {

    private final ChannelDeliveryService delivery;
    private final StatusPublisher status;

    /** Non-blocking retries with exponential backoff and jitter; exhausted messages land in the -dlt topic. */
    @RetryableTopic(
            attempts = "${notify.retry.attempts:5}",
            backoff = @Backoff(delay = 2000, multiplier = 3.0, maxDelay = 120000, random = true),
            exclude = PermanentDeliveryException.class)
    @KafkaListener(topics = {TopicNames.WEBHOOK_HIGH, TopicNames.WEBHOOK_STD}, groupId = "worker-webhook")
    @IdempotentNotification(keyExpression = "#event.notificationId().toString() + ':WEBHOOK'")
    public void onMessage(NotificationEvent event) {
        delivery.deliver(event, Channel.WEBHOOK);
    }

    @DltHandler
    public void onDeadLetter(NotificationEvent event,
                             @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String reason) {
        status.publish(event, Channel.WEBHOOK, DeliveryStatus.FAILED, "n/a", null, "DLT: " + reason);
    }
}
