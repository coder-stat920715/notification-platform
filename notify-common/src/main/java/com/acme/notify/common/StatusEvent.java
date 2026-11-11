package com.acme.notify.common;

import java.time.Instant;
import java.util.UUID;

public record StatusEvent(
        UUID notificationId,
        String userId,
        Channel channel,
        DeliveryStatus status,
        String provider,
        String providerMessageId,
        int attempt,
        String errorCode,
        Instant occurredAt) {}
