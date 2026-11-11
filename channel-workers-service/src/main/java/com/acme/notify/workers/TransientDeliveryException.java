package com.acme.notify.workers;

/** Retryable failure: the message goes to the next retry topic. */
public class TransientDeliveryException extends RuntimeException {
    public TransientDeliveryException(String message) { super(message); }
}
