package io.github.sungjh.aiusagebillinggateway.organization;

import io.github.sungjh.aiusagebillinggateway.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @Email @NotBlank String email,
        @NotNull Role role) {
}
