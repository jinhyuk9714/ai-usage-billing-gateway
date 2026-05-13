package io.github.sungjh.aiusagebillinggateway.organization;

import io.github.sungjh.aiusagebillinggateway.security.SecurityPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.create(SecurityPrincipal.currentUserId(), request);
    }

    @GetMapping
    List<OrganizationResponse> mine() {
        return organizationService.mine(SecurityPrincipal.currentUserId());
    }

    @GetMapping("/{orgId}")
    OrganizationResponse get(@PathVariable UUID orgId) {
        return organizationService.get(SecurityPrincipal.currentUserId(), orgId);
    }

    @PostMapping("/{orgId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    MemberResponse addMember(
            @PathVariable UUID orgId,
            @Valid @RequestBody AddMemberRequest request) {
        return organizationService.addMember(SecurityPrincipal.currentUserId(), orgId, request);
    }

    @PutMapping("/{orgId}/subscription")
    SubscriptionResponse changeSubscription(
            @PathVariable UUID orgId,
            @Valid @RequestBody ChangeSubscriptionRequest request) {
        return organizationService.changeSubscription(SecurityPrincipal.currentUserId(), orgId, request);
    }
}
