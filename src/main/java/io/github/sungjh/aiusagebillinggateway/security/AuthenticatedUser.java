package io.github.sungjh.aiusagebillinggateway.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email) {
}
