package com.acme.notify.workers.handler;

import com.acme.notify.common.Channel;
import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.provider.ProviderRouter;
import com.acme.notify.workers.provider.PushProvider;
import org.springframework.stereotype.Component;

@Component
public class PushChannelHandler implements NotificationChannelHandler {
    private final ProviderRouter<PushProvider> router;
    public PushChannelHandler(ProviderRouter<PushProvider> router) { this.router = router; }
    @Override public Channel channel() { return Channel.PUSH; }
    @Override public DeliveryResult send(RenderedNotification n) { return router.execute(p -> p.send(n)); }
}
