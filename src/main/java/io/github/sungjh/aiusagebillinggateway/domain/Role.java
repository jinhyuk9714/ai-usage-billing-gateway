package io.github.sungjh.aiusagebillinggateway.domain;

public enum Role {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean canAdministerBilling() {
        return this == OWNER || this == ADMIN;
    }
}
