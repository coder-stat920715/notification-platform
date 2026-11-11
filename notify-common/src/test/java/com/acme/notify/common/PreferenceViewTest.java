package com.acme.notify.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreferenceViewTest {

    private final PreferenceView view = new PreferenceView("u1", "Asia/Kolkata", "en",
            Map.of(Channel.EMAIL, Map.of(Category.MARKETING,
                    new PreferenceView.Rule(true, LocalTime.of(22, 0), LocalTime.of(7, 0), 3))));

    @Test
    void overnightQuietWindowIsHonoured() {
        Instant elevenPmIst = Instant.parse("2026-01-01T17:30:00Z");
        Instant noonIst = Instant.parse("2026-01-01T06:30:00Z");
        assertTrue(view.inQuietHours(Channel.EMAIL, Category.MARKETING, elevenPmIst));
        assertFalse(view.inQuietHours(Channel.EMAIL, Category.MARKETING, noonIst));
    }

    @Test
    void nextWindowStartIsNextMorning() {
        Instant elevenPmIst = Instant.parse("2026-01-01T17:30:00Z");
        assertEquals(Instant.parse("2026-01-02T01:30:00Z"),
                view.nextWindowStart(Channel.EMAIL, Category.MARKETING, elevenPmIst));
    }

    @Test
    void capsFallBackToDefault() {
        assertEquals(3, view.capFor(Channel.EMAIL, Category.MARKETING));
        assertEquals(PreferenceView.DEFAULT_HOURLY_CAP, view.capFor(Channel.SMS, Category.OTP));
    }

    @Test
    void unknownRulesAreOptIn() {
        assertTrue(view.isEnabled(Channel.PUSH, Category.TRANSACTIONAL));
    }
}
