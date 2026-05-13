package io.github.sungjh.aiusagebillinggateway.organization;

import io.github.sungjh.aiusagebillinggateway.domain.Role;
import java.util.UUID;

public record MemberResponse(UUID id, UUID userId, String email, Role role) {
}
