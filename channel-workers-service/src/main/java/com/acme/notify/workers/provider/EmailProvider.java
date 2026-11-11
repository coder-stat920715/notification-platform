package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;

public interface EmailProvider extends NotificationProvider {
    DeliveryResult send(RenderedNotification notification);
}
