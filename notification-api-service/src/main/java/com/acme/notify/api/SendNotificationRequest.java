package com.acme.notify.api;

import com.acme.notify.common.Category;
import com.acme.notify.common.Channel;
import com.acme.notify.common.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record SendNotificationRequest(
        @NotBlank String userId,
        @NotBlank String templateKey,
        @NotNull Priority priority,
        @NotNull Category category,
        @NotEmpty Set<Channel> channels,
        @Pattern(regexp = "[a-z]{2}(-[A-Z]{2})?", message = "locale must look like en or en-US") String locale,
        Map<String, Object> variables,
        Instant scheduledAt) {}
