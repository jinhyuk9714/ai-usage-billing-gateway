package io.github.sungjh.aiusagebillinggateway.security;

import java.util.UUID;

public record AuthenticatedApiKey(UUID organizationId, UUID apiKeyId, String keyPrefix) {
}
