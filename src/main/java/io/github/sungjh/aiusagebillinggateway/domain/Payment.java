package io.github.sungjh.aiusagebillinggateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "provider_event_id", nullable = false, unique = true)
    private String providerEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(
            UUID organizationId,
            UUID invoiceId,
            String providerEventId,
            PaymentStatus status,
            long amountMinor,
            String currency) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.invoiceId = invoiceId;
        this.providerEventId = providerEventId;
        this.status = status;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }
}
