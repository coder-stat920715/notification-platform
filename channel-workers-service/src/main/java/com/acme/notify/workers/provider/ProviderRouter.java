package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.TransientProviderException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Ordered failover across vendors. PermanentDeliveryException propagates untouched (a bad phone number
 * will fail on every vendor); open breakers and transient errors fall through to the next vendor.
 */
@Slf4j
public class ProviderRouter<P extends NotificationProvider> {

    private final String name;
    private final List<P> providers;

    public ProviderRouter(String name, List<P> providers) {
        this.name = name;
        this.providers = List.copyOf(providers);
    }

    public DeliveryResult execute(Function<P, DeliveryResult> call) {
        DeliveryResult last = null;
        for (P provider : providers) {
            if (!provider.isEnabled()) continue;
            try {
                last = call.apply(provider);
                if (last.status() == DeliveryResult.Status.SENT) return last;
                log.warn("[{}] provider {} returned retryable failure: {}", name, provider.name(), last.error());
            } catch (CallNotPermittedException | TransientProviderException e) {
                log.warn("[{}] provider {} unavailable, failing over: {}", name, provider.name(), e.getMessage());
                last = DeliveryResult.retryable(provider.name(), e.getMessage());
            }
        }
        return last != null ? last : DeliveryResult.retryable("none", "No enabled provider for " + name);
    }
}
