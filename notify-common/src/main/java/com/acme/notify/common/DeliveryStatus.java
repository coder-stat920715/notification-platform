package com.acme.notify.common;

public enum DeliveryStatus {
    PENDING, PROCESSING, SENT, DELIVERED, READ, FAILED;

    /** Forward-only state machine; stale or out-of-order events are rejected. */
    public boolean canTransitionTo(DeliveryStatus next) {
        return switch (this) {
            case PENDING -> next == PROCESSING || next == FAILED;
            case PROCESSING -> next == SENT || next == FAILED;
            case SENT -> next == DELIVERED || next == FAILED;
            case DELIVERED -> next == READ;
            case READ, FAILED -> false;
        };
    }
}
