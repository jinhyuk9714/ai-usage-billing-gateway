package io.github.sungjh.aiusagebillinggateway.apikey;

import io.github.sungjh.aiusagebillinggateway.domain.ApiKey;
import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        String status,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt) {

    public static ApiKeyResponse from(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getStatus().name(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt());
    }
}
