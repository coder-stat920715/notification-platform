package com.acme.notify.api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_event", indexes = @Index(name = "idx_outbox_pending", columnList = "published,id"))
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateId;
    private String partitionKey;
    private String topic;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    private boolean published;
    private Instant createdAt = Instant.now();

    public static OutboxEvent of(String aggregateId, String partitionKey, String topic, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.aggregateId = aggregateId;
        e.partitionKey = partitionKey;
        e.topic = topic;
        e.payload = payload;
        return e;
    }
}
