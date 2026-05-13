package io.github.sungjh.aiusagebillinggateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "included_quantity", nullable = false)
    private long includedQuantity;

    @Column(name = "overage_unit_amount_minor", nullable = false)
    private long overageUnitAmountMinor;

    @Column(name = "base_amount_minor", nullable = false)
    private long baseAmountMinor;

    @Column(nullable = false)
    private String currency;

    @Column(name = "overage_allowed", nullable = false)
    private boolean overageAllowed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Plan() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public long getIncludedQuantity() {
        return includedQuantity;
    }

    public long getOverageUnitAmountMinor() {
        return overageUnitAmountMinor;
    }

    public long getBaseAmountMinor() {
        return baseAmountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isOverageAllowed() {
        return overageAllowed;
    }
}
