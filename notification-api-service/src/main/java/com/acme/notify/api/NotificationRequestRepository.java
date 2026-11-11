package com.acme.notify.api;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, UUID> {
    Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey);
}
