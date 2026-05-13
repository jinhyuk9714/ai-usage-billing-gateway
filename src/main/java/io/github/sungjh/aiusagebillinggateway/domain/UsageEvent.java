package io.github.sungjh.aiusagebillinggateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usage_events")
public class UsageEvent {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "api_key_id", nullable = false)
    private UUID apiKeyId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsageMetric metric;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UsageEvent() {
    }

    public UsageEvent(
            UUID organizationId,
            UUID apiKeyId,
            String idempotencyKey,
            String requestHash,
            UsageMetric metric,
            long quantity,
            Instant occurredAt,
            String metadata) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.apiKeyId = apiKeyId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.metric = metric;
        this.quantity = quantity;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getRequestHash() {
        return requestHash;
    }
}
