package com.acme.notify.workers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Guards a consumer method against duplicate execution caused by at-least-once redelivery. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentNotification {
    /** SpEL evaluated over the method arguments, e.g. "#event.notificationId().toString() + ':EMAIL'". */
    String keyExpression();

    long doneTtlSeconds() default 86_400;

    /** Lease for the in-flight marker; short, so a crashed worker's message becomes retryable quickly. */
    long processingLeaseSeconds() default 60;
}
