package com.acme.notify.preference;

import com.acme.notify.common.Category;
import com.acme.notify.common.Channel;
import com.acme.notify.common.ContactView;
import com.acme.notify.common.PreferenceView;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final PreferenceRepository preferences;
    private final ContactRepository contacts;

    @Transactional(readOnly = true)
    public PreferenceView view(String userId) {
        List<UserNotificationPreference> list = preferences.findByUserId(userId);
        if (list.isEmpty()) return PreferenceView.defaults(userId);

        Map<Channel, Map<Category, PreferenceView.Rule>> rules = new EnumMap<>(Channel.class);
        for (UserNotificationPreference p : list) {
            rules.computeIfAbsent(p.getChannel(), k -> new EnumMap<>(Category.class))
                    .put(p.getCategory(), new PreferenceView.Rule(
                            p.isEnabled(), p.getQuietStart(), p.getQuietEnd(), p.getMaxPerHour()));
        }
        UserNotificationPreference first = list.get(0);
        return new PreferenceView(userId, first.getTimeZone(), first.getPreferredLocale(), rules);
    }

    @Transactional
    public void upsert(PreferenceUpdate u) {
        UserNotificationPreference p = preferences
                .findByUserIdAndChannelAndCategory(u.userId(), u.channel(), u.category())
                .orElseGet(UserNotificationPreference::new);
        p.setUserId(u.userId());
        p.setChannel(u.channel());
        p.setCategory(u.category());
        p.setEnabled(u.enabled());
        p.setQuietStart(u.quietStart());
        p.setQuietEnd(u.quietEnd());
        if (u.timeZone() != null) p.setTimeZone(u.timeZone());
        p.setMaxPerHour(u.maxPerHour());
        if (u.preferredLocale() != null) p.setPreferredLocale(u.preferredLocale());
        preferences.save(p);
    }

    @Transactional(readOnly = true)
    public ContactView contact(String userId) {
        return contacts.findById(userId)
                .map(c -> new ContactView(c.getUserId(), c.getEmail(), c.getPhone(), c.getDeviceToken(), c.getWebhookUrl()))
                .orElse(new ContactView(userId, null, null, null, null));
    }

    @Transactional
    public void saveContact(ContactView v) {
        UserContact c = contacts.findById(v.userId()).orElseGet(UserContact::new);
        c.setUserId(v.userId());
        c.setEmail(v.email());
        c.setPhone(v.phone());
        c.setDeviceToken(v.deviceToken());
        c.setWebhookUrl(v.webhookUrl());
        contacts.save(c);
    }
}
