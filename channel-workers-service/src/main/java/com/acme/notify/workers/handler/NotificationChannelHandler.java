package com.acme.notify.workers.handler;

import com.acme.notify.common.Channel;
import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;

/** Strategy: one implementation per channel. Adding a channel means adding a @Component. */
public interface NotificationChannelHandler {
    Channel channel();
    DeliveryResult send(RenderedNotification notification);
}
