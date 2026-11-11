package com.acme.notify.common;

import java.util.ArrayList;
import java.util.List;

public final class TopicNames {
    private TopicNames() {}

    public static final String REQ_HIGH = "notif.requests.high";
    public static final String REQ_MEDIUM = "notif.requests.medium";
    public static final String REQ_BATCH = "notif.requests.batch";
    public static final String STATUS = "notif.events.status";

    public static final String EMAIL_HIGH = "notif.channel.email.high";
    public static final String EMAIL_STD = "notif.channel.email.std";
    public static final String SMS_HIGH = "notif.channel.sms.high";
    public static final String SMS_STD = "notif.channel.sms.std";
    public static final String PUSH_HIGH = "notif.channel.push.high";
    public static final String PUSH_STD = "notif.channel.push.std";
    public static final String IN_APP_HIGH = "notif.channel.in_app.high";
    public static final String IN_APP_STD = "notif.channel.in_app.std";
    public static final String WEBHOOK_HIGH = "notif.channel.webhook.high";
    public static final String WEBHOOK_STD = "notif.channel.webhook.std";

    public static String channel(Channel channel, Priority priority) {
        return "notif.channel.%s.%s".formatted(channel.name().toLowerCase(),
                priority == Priority.HIGH ? "high" : "std");
    }

    public static List<String> allChannelTopics() {
        List<String> topics = new ArrayList<>();
        for (Channel c : Channel.values()) {
            topics.add(channel(c, Priority.HIGH));
            topics.add(channel(c, Priority.MEDIUM));
        }
        return topics;
    }
}
