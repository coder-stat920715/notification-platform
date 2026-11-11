package com.acme.notify.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationStatusView(UUID notificationId,
                                     Map<Channel, DeliveryStatus> perChannel,
                                     List<Entry> history) {
    public record Entry(Channel channel, DeliveryStatus status, String provider,
                        int attempt, Instant at, String error) {}
}
