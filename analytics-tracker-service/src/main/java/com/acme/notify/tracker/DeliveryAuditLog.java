package com.acme.notify.tracker;

import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only audit trail. PostgreSQL here for a runnable default; at scale, store this in Cassandra
 * (partition by notification_id) and mirror to Elasticsearch for search.
 */
@Entity
@Table(name = "delivery_audit_log", indexes = {
        @Index(name = "idx_audit_notification", columnList = "notification_id,id"),
        @Index(name = "idx_audit_user_time", columnList = "user_id,occurred_at")})
@Getter
@Setter
@NoArgsConstructor
public class DeliveryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id")
    private UUID notificationId;
    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private String provider;
    private String providerMessageId;
    private int attempt;
    private String errorCode;
    @Column(name = "occurred_at")
    private Instant occurredAt;
}
