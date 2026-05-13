package io.github.sungjh.aiusagebillinggateway.apikey;

import java.util.UUID;

public record CreateApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        String rawApiKey,
        String status) {
}
