package com.acme.notify.preference;

import com.acme.notify.common.Category;
import com.acme.notify.common.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record PreferenceUpdate(
        @NotBlank String userId,
        @NotNull Channel channel,
        @NotNull Category category,
        boolean enabled,
        LocalTime quietStart,
        LocalTime quietEnd,
        String timeZone,
        Integer maxPerHour,
        String preferredLocale) {}
