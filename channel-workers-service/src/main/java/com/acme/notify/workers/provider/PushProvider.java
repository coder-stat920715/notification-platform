package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;

public interface PushProvider extends NotificationProvider {
    DeliveryResult send(RenderedNotification notification);
}
