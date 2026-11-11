package com.acme.notify.tracker;

import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.NotificationStatusView;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/status")
@RequiredArgsConstructor
public class StatusController {

    private final AuditRepository audit;

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public NotificationStatusView status(@PathVariable UUID id) {
        List<DeliveryAuditLog> rows = audit.findByNotificationIdOrderByIdAsc(id);
        Map<Channel, DeliveryStatus> perChannel = new EnumMap<>(Channel.class);
        rows.forEach(r -> perChannel.put(r.getChannel(), r.getStatus()));   // ordered, so last write wins
        List<NotificationStatusView.Entry> history = rows.stream()
                .map(r -> new NotificationStatusView.Entry(r.getChannel(), r.getStatus(), r.getProvider(),
                        r.getAttempt(), r.getOccurredAt(), r.getErrorCode()))
                .toList();
        return new NotificationStatusView(id, perChannel, history);
    }
}
