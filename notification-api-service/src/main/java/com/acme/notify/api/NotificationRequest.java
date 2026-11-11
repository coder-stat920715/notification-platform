package com.acme.notify.api;

import com.acme.notify.common.Category;
import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.Priority;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_request")
@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String templateKey;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String locale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> variables;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_request_channel", joinColumns = @JoinColumn(name = "request_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private Set<Channel> channels = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private Instant scheduledAt;

    private Instant createdAt;

    public static NotificationRequest from(UUID id, String idempotencyKey, SendNotificationRequest r) {
        NotificationRequest n = new NotificationRequest();
        n.id = id;
        n.idempotencyKey = idempotencyKey;
        n.userId = r.userId();
        n.templateKey = r.templateKey();
        n.priority = r.priority();
        n.category = r.category();
        n.locale = r.locale() == null ? "en" : r.locale();
        n.variables = r.variables();
        n.channels = new HashSet<>(r.channels());
        n.status = DeliveryStatus.PENDING;
        n.scheduledAt = r.scheduledAt();
        n.createdAt = Instant.now();
        return n;
    }
}
