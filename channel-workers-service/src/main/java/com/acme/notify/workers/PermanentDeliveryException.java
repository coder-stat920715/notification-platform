package com.acme.notify.workers;

/** Non-retryable failure (bad address, unsubscribed, 4xx): mark FAILED and move on. */
public class PermanentDeliveryException extends RuntimeException {
    public PermanentDeliveryException(String message) { super(message); }
}
