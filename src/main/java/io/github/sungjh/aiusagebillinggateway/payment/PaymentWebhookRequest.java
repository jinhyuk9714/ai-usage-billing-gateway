package io.github.sungjh.aiusagebillinggateway.payment;

import java.util.UUID;

public record PaymentWebhookRequest(
        String providerEventId,
        String type,
        UUID invoiceId,
        long amountMinor,
        String currency) {
}
