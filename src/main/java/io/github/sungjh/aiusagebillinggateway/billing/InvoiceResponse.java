package io.github.sungjh.aiusagebillinggateway.billing;

import io.github.sungjh.aiusagebillinggateway.domain.Invoice;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID organizationId,
        String billingPeriod,
        String status,
        long totalAmountMinor,
        String currency,
        boolean duplicate) {

    public static InvoiceResponse from(Invoice invoice, boolean duplicate) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrganizationId(),
                invoice.getBillingPeriod(),
                invoice.getStatus().name(),
                invoice.getTotalAmountMinor(),
                invoice.getCurrency(),
                duplicate);
    }
}
