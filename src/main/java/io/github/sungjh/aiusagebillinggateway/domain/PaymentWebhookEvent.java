package io.github.sungjh.aiusagebillinggateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent {

    @Id
    private UUID id;

    @Column(name = "provider_event_id", nullable = false, unique = true)
    private String providerEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(nullable = false)
    private boolean duplicate;

    protected PaymentWebhookEvent() {
    }

    public PaymentWebhookEvent(String providerEventId, String eventType, String payloadHash) {
        this.id = UUID.randomUUID();
        this.providerEventId = providerEventId;
        this.eventType = eventType;
        this.payloadHash = payloadHash;
        this.processedAt = Instant.now();
        this.duplicate = false;
    }

    public String getPayloadHash() {
        return payloadHash;
    }
}
