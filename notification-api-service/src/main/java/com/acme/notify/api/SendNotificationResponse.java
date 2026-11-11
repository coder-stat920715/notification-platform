package com.acme.notify.api;

import com.acme.notify.common.DeliveryStatus;
import java.util.UUID;

public record SendNotificationResponse(UUID notificationId, DeliveryStatus status) {}
