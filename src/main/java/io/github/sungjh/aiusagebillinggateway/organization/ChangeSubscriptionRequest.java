package io.github.sungjh.aiusagebillinggateway.organization;

import jakarta.validation.constraints.NotBlank;

public record ChangeSubscriptionRequest(@NotBlank String planCode) {
}
