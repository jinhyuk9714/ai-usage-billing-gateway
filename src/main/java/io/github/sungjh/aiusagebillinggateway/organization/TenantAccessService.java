package io.github.sungjh.aiusagebillinggateway.organization;

import io.github.sungjh.aiusagebillinggateway.domain.OrganizationMember;
import io.github.sungjh.aiusagebillinggateway.domain.Role;
import io.github.sungjh.aiusagebillinggateway.repository.OrganizationMemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantAccessService {

    private final OrganizationMemberRepository memberRepository;

    public TenantAccessService(OrganizationMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Role requireMember(UUID organizationId, UUID userId) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "User is not a member of this organization"));
        return member.getRole();
    }

    public void requireAdmin(UUID organizationId, UUID userId) {
        Role role = requireMember(organizationId, userId);
        if (!role.canAdministerBilling()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
