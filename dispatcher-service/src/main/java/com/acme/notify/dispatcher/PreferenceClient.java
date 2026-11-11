package com.acme.notify.dispatcher;

import com.acme.notify.common.ContactView;
import com.acme.notify.common.PreferenceView;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cached client to preference-service. Degrades open (default opt-in, UTC) if the service is down. */
@Component
@Slf4j
public class PreferenceClient {

    private final RestClient client;

    public PreferenceClient(RestClient.Builder builder, @Value("${notify.services.preference-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Cacheable(value = "prefs", key = "#userId")
    @CircuitBreaker(name = "preferences", fallbackMethod = "prefsFallback")
    public PreferenceView preferences(String userId) {
        return client.get().uri("/internal/preferences/{u}", userId).retrieve().body(PreferenceView.class);
    }

    @Cacheable(value = "contacts", key = "#userId")
    @CircuitBreaker(name = "preferences", fallbackMethod = "contactFallback")
    public ContactView contact(String userId) {
        return client.get().uri("/internal/contacts/{u}", userId).retrieve().body(ContactView.class);
    }

    public PreferenceView prefsFallback(String userId, Throwable t) {
        log.warn("Preference lookup failed for {}, using defaults: {}", userId, t.toString());
        return PreferenceView.defaults(userId);
    }

    public ContactView contactFallback(String userId, Throwable t) {
        log.warn("Contact lookup failed for {}: {}", userId, t.toString());
        return new ContactView(userId, null, null, null, null);
    }
}
