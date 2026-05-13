package io.github.sungjh.aiusagebillinggateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "unit_amount_minor", nullable = false)
    private long unitAmountMinor;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvoiceItem() {
    }

    public InvoiceItem(UUID invoiceId, String description, long quantity, long unitAmountMinor) {
        this.id = UUID.randomUUID();
        this.invoiceId = invoiceId;
        this.description = description;
        this.quantity = quantity;
        this.unitAmountMinor = unitAmountMinor;
        this.amountMinor = quantity * unitAmountMinor;
        this.createdAt = Instant.now();
    }
}
