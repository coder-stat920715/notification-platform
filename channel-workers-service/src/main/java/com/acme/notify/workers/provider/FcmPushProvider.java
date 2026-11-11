package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.PermanentDeliveryException;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.TransientProviderException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** FCM delivers to Android, web and iOS (iOS via Apple's APNs relay configured in Firebase). */
@Component
@Order(1)
@ConditionalOnProperty(name = "notify.providers.mode", havingValue = "live")
public class FcmPushProvider implements PushProvider {

    private final FirebaseMessaging fcm;

    public FcmPushProvider(FirebaseMessaging fcm) { this.fcm = fcm; }

    @Override public String name() { return "fcm"; }

    @Override
    @CircuitBreaker(name = "fcm", fallbackMethod = "fallback")
    public DeliveryResult send(RenderedNotification n) {
        try {
            Message message = Message.builder()
                    .setToken(n.recipient())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(n.subject()).setBody(n.body()).build())
                    .putData("notificationId", n.notificationId())
                    .build();
            return DeliveryResult.sent(name(), fcm.send(message));
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT
                    || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
                throw new PermanentDeliveryException("FCM token rejected: " + code);   // also purge the stale token
            }
            throw new TransientProviderException("FCM error: " + e.getMessage());
        }
    }

    public DeliveryResult fallback(RenderedNotification n, PermanentDeliveryException e) { throw e; }

    public DeliveryResult fallback(RenderedNotification n, Throwable t) {
        return DeliveryResult.retryable(name(), t.getMessage());
    }
}
