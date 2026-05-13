package io.github.sungjh.aiusagebillinggateway.organization;

import io.github.sungjh.aiusagebillinggateway.domain.Organization;
import java.util.UUID;

public record OrganizationResponse(UUID id, String name) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(organization.getId(), organization.getName());
    }
}
