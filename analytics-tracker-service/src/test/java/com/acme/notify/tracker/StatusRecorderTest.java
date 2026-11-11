package com.acme.notify.tracker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.StatusEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatusRecorderTest {

    private final AuditRepository repo = mock(AuditRepository.class);
    private final StatusRecorder recorder = new StatusRecorder(repo);
    private final UUID id = UUID.randomUUID();

    private StatusEvent event(DeliveryStatus s) {
        return new StatusEvent(id, "u1", Channel.EMAIL, s, "p", null, 1, null, Instant.now());
    }

    private DeliveryAuditLog row(DeliveryStatus s) {
        DeliveryAuditLog r = new DeliveryAuditLog();
        r.setStatus(s);
        return r;
    }

    @Test
    void acceptsForwardTransition() {
        when(repo.findTopByNotificationIdAndChannelOrderByIdDesc(id, Channel.EMAIL))
                .thenReturn(Optional.of(row(DeliveryStatus.PROCESSING)));
        assertTrue(recorder.record(event(DeliveryStatus.SENT)));
        verify(repo).save(any(DeliveryAuditLog.class));
    }

    @Test
    void ignoresDuplicateAndStaleEvents() {
        when(repo.findTopByNotificationIdAndChannelOrderByIdDesc(id, Channel.EMAIL))
                .thenReturn(Optional.of(row(DeliveryStatus.DELIVERED)));
        assertFalse(recorder.record(event(DeliveryStatus.DELIVERED)));
        assertFalse(recorder.record(event(DeliveryStatus.SENT)));
        verify(repo, never()).save(any(DeliveryAuditLog.class));
    }
}
