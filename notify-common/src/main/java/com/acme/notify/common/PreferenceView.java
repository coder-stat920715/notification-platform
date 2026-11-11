package com.acme.notify.common;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

/** Immutable snapshot of a user's preferences, cached by the dispatcher. */
public record PreferenceView(String userId, String zoneId, String locale,
                             Map<Channel, Map<Category, Rule>> rules) {

    public static final int DEFAULT_HOURLY_CAP = 10;

    public record Rule(boolean enabled, LocalTime quietStart, LocalTime quietEnd, Integer maxPerHour) {}

    public static PreferenceView defaults(String userId) {
        return new PreferenceView(userId, "UTC", "en", Map.of());
    }

    public boolean isEnabled(Channel channel, Category category) {
        Rule r = rule(channel, category);
        return r == null || r.enabled();            // default opt-in
    }

    public boolean inQuietHours(Channel channel, Category category, Instant now) {
        Rule r = rule(channel, category);
        if (r == null || r.quietStart() == null || r.quietEnd() == null) return false;
        LocalTime t = now.atZone(zone()).toLocalTime();
        return r.quietStart().isBefore(r.quietEnd())
                ? !t.isBefore(r.quietStart()) && t.isBefore(r.quietEnd())
                : !t.isBefore(r.quietStart()) || t.isBefore(r.quietEnd());   // overnight window
    }

    public Instant nextWindowStart(Channel channel, Category category, Instant now) {
        Rule r = rule(channel, category);
        if (r == null || r.quietEnd() == null) return now;
        ZonedDateTime candidate = now.atZone(zone()).with(r.quietEnd());
        if (!candidate.toInstant().isAfter(now)) candidate = candidate.plusDays(1);
        return candidate.toInstant();
    }

    public int capFor(Channel channel, Category category) {
        Rule r = rule(channel, category);
        return r == null || r.maxPerHour() == null ? DEFAULT_HOURLY_CAP : r.maxPerHour();
    }

    private Rule rule(Channel channel, Category category) {
        Map<Category, Rule> byCategory = rules == null ? null : rules.get(channel);
        return byCategory == null ? null : byCategory.get(category);
    }

    private ZoneId zone() {
        try { return ZoneId.of(zoneId); } catch (Exception e) { return ZoneId.of("UTC"); }
    }
}
