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
@Table(name = "invoices")
public class Invoice {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "total_amount_minor", nullable = false)
    private long totalAmountMinor;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Invoice() {
    }

    public Invoice(UUID organizationId, String billingPeriod, long totalAmountMinor, String currency) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.billingPeriod = billingPeriod;
        this.status = InvoiceStatus.ISSUED;
        this.totalAmountMinor = totalAmountMinor;
        this.currency = currency;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public long getTotalAmountMinor() {
        return totalAmountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void markPaymentFailed() {
        this.status = InvoiceStatus.PAYMENT_FAILED;
        this.updatedAt = Instant.now();
    }
}
