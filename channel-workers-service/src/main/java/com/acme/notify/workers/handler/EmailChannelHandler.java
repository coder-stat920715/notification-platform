package com.acme.notify.workers.handler;

import com.acme.notify.common.Channel;
import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.provider.EmailProvider;
import com.acme.notify.workers.provider.ProviderRouter;
import org.springframework.stereotype.Component;

@Component
public class EmailChannelHandler implements NotificationChannelHandler {
    private final ProviderRouter<EmailProvider> router;
    public EmailChannelHandler(ProviderRouter<EmailProvider> router) { this.router = router; }
    @Override public Channel channel() { return Channel.EMAIL; }
    @Override public DeliveryResult send(RenderedNotification n) { return router.execute(p -> p.send(n)); }
}
