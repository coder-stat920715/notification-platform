package com.acme.notify.preference;

import com.acme.notify.common.Category;
import com.acme.notify.common.Channel;
import com.acme.notify.common.ContactView;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Seeds a demo user so the quick-start curl commands work out of the box. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notify.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

    private final PreferenceService service;
    private final ContactRepository contacts;

    @Override
    public void run(String... args) {
        if (contacts.existsById("demo-user")) return;
        service.saveContact(new ContactView("demo-user", "demo@example.com", "+919999999999",
                "demo-device-token", "http://localhost:8085/demo/webhook"));
        service.upsert(new PreferenceUpdate("demo-user", Channel.EMAIL, Category.MARKETING,
                true, LocalTime.of(22, 0), LocalTime.of(7, 0), "Asia/Kolkata", 5, "en"));
        service.upsert(new PreferenceUpdate("demo-user", Channel.SMS, Category.MARKETING,
                false, null, null, "Asia/Kolkata", null, "en"));
    }
}
