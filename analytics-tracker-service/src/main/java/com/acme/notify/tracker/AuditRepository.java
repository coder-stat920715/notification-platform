package com.acme.notify.tracker;

import com.acme.notify.common.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<DeliveryAuditLog, Long> {
    List<DeliveryAuditLog> findByNotificationIdOrderByIdAsc(UUID notificationId);
    Optional<DeliveryAuditLog> findTopByNotificationIdAndChannelOrderByIdDesc(UUID notificationId, Channel channel);
}
