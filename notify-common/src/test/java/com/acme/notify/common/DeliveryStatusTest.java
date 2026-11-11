package com.acme.notify.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeliveryStatusTest {

    @Test
    void happyPathMovesForward() {
        assertTrue(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.PROCESSING));
        assertTrue(DeliveryStatus.PROCESSING.canTransitionTo(DeliveryStatus.SENT));
        assertTrue(DeliveryStatus.SENT.canTransitionTo(DeliveryStatus.DELIVERED));
        assertTrue(DeliveryStatus.DELIVERED.canTransitionTo(DeliveryStatus.READ));
    }

    @Test
    void neverMovesBackwardsOrOutOfTerminalStates() {
        assertFalse(DeliveryStatus.SENT.canTransitionTo(DeliveryStatus.PROCESSING));
        assertFalse(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.SENT));
        assertFalse(DeliveryStatus.READ.canTransitionTo(DeliveryStatus.DELIVERED));
    }
}
