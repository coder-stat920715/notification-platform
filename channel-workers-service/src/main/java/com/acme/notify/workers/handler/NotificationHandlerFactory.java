package com.acme.notify.workers.handler;

import com.acme.notify.common.Channel;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NotificationHandlerFactory {

    private final Map<Channel, NotificationChannelHandler> handlers;

    public NotificationHandlerFactory(List<NotificationChannelHandler> all) {
        this.handlers = all.stream().collect(Collectors.toUnmodifiableMap(
                NotificationChannelHandler::channel, Function.identity()));
    }

    public NotificationChannelHandler forChannel(Channel channel) {
        NotificationChannelHandler h = handlers.get(channel);
        if (h == null) throw new IllegalArgumentException("No handler registered for " + channel);
        return h;
    }
}
