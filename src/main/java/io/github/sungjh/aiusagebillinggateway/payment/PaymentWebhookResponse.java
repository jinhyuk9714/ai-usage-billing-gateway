package io.github.sungjh.aiusagebillinggateway.payment;

public record PaymentWebhookResponse(
        String providerEventId,
        boolean duplicate,
        String status) {
}
