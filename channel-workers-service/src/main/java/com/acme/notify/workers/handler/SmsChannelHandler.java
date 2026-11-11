package com.acme.notify.workers.handler;

import com.acme.notify.common.Channel;
import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.provider.ProviderRouter;
import com.acme.notify.workers.provider.SmsProvider;
import org.springframework.stereotype.Component;

@Component
public class SmsChannelHandler implements NotificationChannelHandler {
    private final ProviderRouter<SmsProvider> router;
    public SmsChannelHandler(ProviderRouter<SmsProvider> router) { this.router = router; }
    @Override public Channel channel() { return Channel.SMS; }
    @Override public DeliveryResult send(RenderedNotification n) { return router.execute(p -> p.send(n)); }
}
