package com.acme.notify.api;

import com.acme.notify.common.NotificationStatusView;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class StatusClient {

    private final RestClient client;

    public StatusClient(RestClient.Builder builder, @Value("${notify.services.tracker-url}") String trackerUrl) {
        this.client = builder.baseUrl(trackerUrl).build();
    }

    /** Best effort: the API still answers (with PENDING) if the tracker is unavailable. */
    public NotificationStatusView fetch(UUID id) {
        try {
            return client.get().uri("/internal/status/{id}", id).retrieve().body(NotificationStatusView.class);
        } catch (Exception e) {
            log.warn("Tracker unavailable for {}: {}", id, e.getMessage());
            return null;
        }
    }
}
