package io.github.sungjh.aiusagebillinggateway.organization;

import java.util.UUID;

public record SubscriptionResponse(UUID organizationId, String planCode, String status) {
}
