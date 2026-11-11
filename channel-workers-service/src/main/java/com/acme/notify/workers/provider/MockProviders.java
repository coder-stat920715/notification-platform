package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.RenderedNotification;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Log-only providers used when notify.providers.mode=mock (the default), so the whole pipeline can run
 * locally without any vendor credentials. Set notify.providers.mock-failure-rate to simulate outages.
 */
@Configuration
@ConditionalOnProperty(name = "notify.providers.mode", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockProviders {

    @Value("${notify.providers.mock-failure-rate:0.0}")
    private double failureRate;

    private DeliveryResult simulate(String provider, RenderedNotification n) {
        if (Math.random() < failureRate) {
            return DeliveryResult.retryable(provider, "simulated outage");
        }
        log.info("[MOCK {}] to={} subject='{}' body='{}'", provider, n.recipient(), n.subject(), n.body());
        return DeliveryResult.sent(provider, UUID.randomUUID().toString());
    }

    @Bean
    EmailProvider mockEmailProvider() {
        return new EmailProvider() {
            public String name() { return "mock-email"; }
            public DeliveryResult send(RenderedNotification n) { return simulate(name(), n); }
        };
    }

    @Bean
    SmsProvider mockSmsProvider() {
        return new SmsProvider() {
            public String name() { return "mock-sms"; }
            public DeliveryResult send(RenderedNotification n) { return simulate(name(), n); }
        };
    }

    @Bean
    PushProvider mockPushProvider() {
        return new PushProvider() {
            public String name() { return "mock-push"; }
            public DeliveryResult send(RenderedNotification n) { return simulate(name(), n); }
        };
    }
}
