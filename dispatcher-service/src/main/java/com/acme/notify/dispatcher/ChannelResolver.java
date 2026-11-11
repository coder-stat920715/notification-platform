package com.acme.notify.dispatcher;

import com.acme.notify.common.Channel;
import com.acme.notify.common.NotificationEvent;
import com.acme.notify.common.PreferenceView;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ChannelResolver {

    /** Applies opt-outs (critical categories are never opted out) and drops channels with no address. */
    public Set<Channel> resolve(NotificationEvent event, PreferenceView prefs, Map<Channel, String> recipients) {
        Set<Channel> result = EnumSet.noneOf(Channel.class);
        for (Channel ch : event.channels()) {
            boolean allowed = event.category().isCritical() || prefs.isEnabled(ch, event.category());
            if (allowed && recipients.containsKey(ch)) result.add(ch);
        }
        return result;
    }
}
