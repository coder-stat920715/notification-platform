package com.acme.notify.common;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The message that flows through request topics and channel topics. */
public record NotificationEvent(
        UUID notificationId,
        String userId,
        String templateKey,
        Priority priority,
        Category category,
        Set<Channel> channels,
        String locale,
        Map<String, Object> variables,
        Map<Channel, String> recipients,
        Instant scheduledAt,
        Instant createdAt) {

    public NotificationEvent withRecipients(Map<Channel, String> newRecipients) {
        return new NotificationEvent(notificationId, userId, templateKey, priority, category,
                channels, locale, variables, newRecipients, scheduledAt, createdAt);
    }

    /** Copy narrowed to a single channel, used when publishing to channel topics. */
    public NotificationEvent forChannel(Channel channel) {
        return new NotificationEvent(notificationId, userId, templateKey, priority, category,
                Set.of(channel), locale, variables, recipients, scheduledAt, createdAt);
    }
}
