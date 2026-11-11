package com.acme.notify.workers;

import com.acme.notify.common.Channel;
import com.acme.notify.common.Priority;

public record RenderedNotification(String notificationId, String userId, Channel channel,
                                   String recipient, String subject, String body, Priority priority) {}
