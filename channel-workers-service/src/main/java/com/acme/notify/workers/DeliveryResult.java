package com.acme.notify.workers;

public record DeliveryResult(Status status, String provider, String providerMessageId, String error) {

    public enum Status { SENT, RETRYABLE_FAILURE }

    public static DeliveryResult sent(String provider, String providerMessageId) {
        return new DeliveryResult(Status.SENT, provider, providerMessageId, null);
    }

    public static DeliveryResult retryable(String provider, String error) {
        return new DeliveryResult(Status.RETRYABLE_FAILURE, provider, null, error);
    }
}
