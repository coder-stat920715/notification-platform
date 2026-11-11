package com.acme.notify.workers;

import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.RenderRequest;
import com.acme.notify.common.RenderedContent;
import com.acme.notify.workers.handler.NotificationHandlerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Renders and sends one notification on one channel. Returns normally when the message reached a terminal
 * state (SENT, or permanently FAILED). Throws TransientDeliveryException to trigger a retry-topic hop.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelDeliveryService {

    private final TemplateClient templates;
    private final NotificationHandlerFactory factory;
    private final StatusPublisher status;

    public void deliver(NotificationEvent event, Channel channel) {
        String recipient = event.recipients() == null ? null : event.recipients().get(channel);
        if (recipient == null) {
            status.publish(event, channel, DeliveryStatus.FAILED, "n/a", null, "NO_RECIPIENT");
            return;
        }
        status.publish(event, channel, DeliveryStatus.PROCESSING, null, null, null);
        try {
            RenderedContent content = templates.render(new RenderRequest(
                    event.templateKey(), channel, event.locale(), event.variables()));
            RenderedNotification notification = new RenderedNotification(event.notificationId().toString(),
                    event.userId(), channel, recipient, content.subject(), content.body(), event.priority());

            DeliveryResult result = factory.forChannel(channel).send(notification);
            if (result.status() == DeliveryResult.Status.SENT) {
                status.publish(event, channel, DeliveryStatus.SENT, result.provider(), result.providerMessageId(), null);
            } else {
                throw new TransientDeliveryException("All providers failed: " + result.error());
            }
        } catch (PermanentDeliveryException e) {
            log.warn("Permanent failure id={} channel={}: {}", event.notificationId(), channel, e.getMessage());
            status.publish(event, channel, DeliveryStatus.FAILED, "n/a", null, e.getMessage());
        }
    }
}
