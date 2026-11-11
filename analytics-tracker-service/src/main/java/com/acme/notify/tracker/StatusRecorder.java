package com.acme.notify.tracker;

import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.StatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the delivery state machine: forward-only, duplicate and out-of-order events are ignored. */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatusRecorder {

    private final AuditRepository audit;

    @Transactional
    public boolean record(StatusEvent e) {
        DeliveryStatus current = audit
                .findTopByNotificationIdAndChannelOrderByIdDesc(e.notificationId(), e.channel())
                .map(DeliveryAuditLog::getStatus)
                .orElse(DeliveryStatus.PENDING);

        if (current == e.status()) return false;                       // duplicate delivery of same event
        if (!current.canTransitionTo(e.status())) {
            log.debug("Ignoring out-of-order {} -> {} for {}", current, e.status(), e.notificationId());
            return false;
        }
        DeliveryAuditLog row = new DeliveryAuditLog();
        row.setNotificationId(e.notificationId());
        row.setUserId(e.userId());
        row.setChannel(e.channel());
        row.setStatus(e.status());
        row.setProvider(e.provider());
        row.setProviderMessageId(e.providerMessageId());
        row.setAttempt(e.attempt());
        row.setErrorCode(e.errorCode());
        row.setOccurredAt(e.occurredAt());
        audit.save(row);
        return true;
    }
}
