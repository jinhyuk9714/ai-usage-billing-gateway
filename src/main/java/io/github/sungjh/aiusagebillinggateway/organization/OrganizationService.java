package io.github.sungjh.aiusagebillinggateway.organization;

import io.github.sungjh.aiusagebillinggateway.audit.AuditService;
import io.github.sungjh.aiusagebillinggateway.domain.Organization;
import io.github.sungjh.aiusagebillinggateway.domain.OrganizationMember;
import io.github.sungjh.aiusagebillinggateway.domain.Plan;
import io.github.sungjh.aiusagebillinggateway.domain.Role;
import io.github.sungjh.aiusagebillinggateway.domain.Subscription;
import io.github.sungjh.aiusagebillinggateway.domain.UserAccount;
import io.github.sungjh.aiusagebillinggateway.repository.OrganizationMemberRepository;
import io.github.sungjh.aiusagebillinggateway.repository.OrganizationRepository;
import io.github.sungjh.aiusagebillinggateway.repository.PlanRepository;
import io.github.sungjh.aiusagebillinggateway.repository.SubscriptionRepository;
import io.github.sungjh.aiusagebillinggateway.repository.UserAccountRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserAccountRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            UserAccountRepository userRepository,
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            TenantAccessService tenantAccessService,
            AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional
    public OrganizationResponse create(UUID userId, CreateOrganizationRequest request) {
        Organization organization = organizationRepository.save(new Organization(request.name()));
        memberRepository.save(new OrganizationMember(organization.getId(), userId, Role.OWNER));
        Plan freePlan = planRepository.findByCode("FREE")
                .orElseThrow(() -> new IllegalStateException("FREE plan missing"));
        subscriptionRepository.save(new Subscription(organization.getId(), freePlan.getId()));
        auditService.record(
                organization.getId(),
                userId,
                "ORGANIZATION_CREATED",
                "Organization",
                organization.getId(),
                Map.of("name", organization.getName()));
        return OrganizationResponse.from(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> mine(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(member -> organizationRepository.findById(member.getOrganizationId()).orElseThrow())
                .map(OrganizationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID userId, UUID organizationId) {
        tenantAccessService.requireMember(organizationId, userId);
        return organizationRepository.findById(organizationId)
                .map(OrganizationResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    @Transactional
    public MemberResponse addMember(UUID actorUserId, UUID organizationId, AddMemberRequest request) {
        tenantAccessService.requireAdmin(organizationId, actorUserId);
        UserAccount user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        OrganizationMember member = memberRepository.save(new OrganizationMember(
                organizationId,
                user.getId(),
                request.role()));
        auditService.record(
                organizationId,
                actorUserId,
                "MEMBER_ADDED",
                "User",
                user.getId(),
                Map.of("role", request.role().name()));
        return new MemberResponse(member.getId(), user.getId(), request.email().toLowerCase(), member.getRole());
    }

    @Transactional
    public SubscriptionResponse changeSubscription(
            UUID actorUserId,
            UUID organizationId,
            ChangeSubscriptionRequest request) {
        tenantAccessService.requireAdmin(organizationId, actorUserId);
        Plan plan = planRepository.findByCode(request.planCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> new Subscription(organizationId, plan.getId()));
        subscription.changePlan(plan.getId());
        subscriptionRepository.save(subscription);
        auditService.record(
                organizationId,
                actorUserId,
                "SUBSCRIPTION_CHANGED",
                "Subscription",
                subscription.getId(),
                Map.of("planCode", plan.getCode()));
        return new SubscriptionResponse(organizationId, plan.getCode(), subscription.getStatus().name());
    }
}
