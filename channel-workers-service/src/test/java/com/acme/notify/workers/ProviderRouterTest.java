package com.acme.notify.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.notify.common.Channel;
import com.acme.notify.common.Priority;
import com.acme.notify.workers.provider.ProviderRouter;
import com.acme.notify.workers.provider.SmsProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderRouterTest {

    private static final RenderedNotification N =
            new RenderedNotification("id-1", "u1", Channel.SMS, "+10000000000", "s", "b", Priority.HIGH);

    private static SmsProvider provider(String name, boolean enabled, java.util.function.Supplier<DeliveryResult> behaviour) {
        return new SmsProvider() {
            public String name() { return name; }
            public boolean isEnabled() { return enabled; }
            public DeliveryResult send(RenderedNotification n) { return behaviour.get(); }
        };
    }

    @Test
    void failsOverWhenPrimaryThrowsTransient() {
        var router = new ProviderRouter<>("sms", List.of(
                provider("twilio", true, () -> { throw new TransientProviderException("503"); }),
                provider("infobip", true, () -> DeliveryResult.sent("infobip", "m-1"))));
        assertEquals("infobip", router.execute(p -> p.send(N)).provider());
    }

    @Test
    void failsOverWhenPrimaryReturnsRetryable() {
        var router = new ProviderRouter<>("sms", List.of(
                provider("twilio", true, () -> DeliveryResult.retryable("twilio", "slow")),
                provider("infobip", true, () -> DeliveryResult.sent("infobip", "m-2"))));
        assertEquals(DeliveryResult.Status.SENT, router.execute(p -> p.send(N)).status());
    }

    @Test
    void skipsDisabledProviders() {
        var router = new ProviderRouter<>("sms", List.of(
                provider("twilio", false, () -> DeliveryResult.sent("twilio", "x")),
                provider("infobip", true, () -> DeliveryResult.sent("infobip", "y"))));
        assertEquals("infobip", router.execute(p -> p.send(N)).provider());
    }

    @Test
    void permanentFailuresDoNotFailOver() {
        var router = new ProviderRouter<>("sms", List.of(
                provider("twilio", true, () -> { throw new PermanentDeliveryException("bad number"); }),
                provider("infobip", true, () -> DeliveryResult.sent("infobip", "z"))));
        assertThrows(PermanentDeliveryException.class, () -> router.execute(p -> p.send(N)));
    }

    @Test
    void returnsRetryableWhenAllProvidersFail() {
        var router = new ProviderRouter<>("sms", List.of(
                provider("twilio", true, () -> { throw new TransientProviderException("down"); })));
        assertEquals(DeliveryResult.Status.RETRYABLE_FAILURE, router.execute(p -> p.send(N)).status());
    }
}
