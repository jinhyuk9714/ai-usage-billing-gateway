package io.github.sungjh.aiusagebillinggateway.apikey;

import io.github.sungjh.aiusagebillinggateway.audit.AuditService;
import io.github.sungjh.aiusagebillinggateway.common.Hashing;
import io.github.sungjh.aiusagebillinggateway.domain.ApiKey;
import io.github.sungjh.aiusagebillinggateway.domain.ApiKeyStatus;
import io.github.sungjh.aiusagebillinggateway.observability.MetricsService;
import io.github.sungjh.aiusagebillinggateway.organization.TenantAccessService;
import io.github.sungjh.aiusagebillinggateway.repository.ApiKeyRepository;
import io.github.sungjh.aiusagebillinggateway.security.AuthenticatedApiKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;
    private final MetricsService metricsService;

    public ApiKeyService(
            ApiKeyRepository apiKeyRepository,
            TenantAccessService tenantAccessService,
            AuditService auditService,
            MetricsService metricsService) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
        this.metricsService = metricsService;
    }

    @Transactional
    public CreateApiKeyResponse create(UUID actorUserId, UUID organizationId, CreateApiKeyRequest request) {
        tenantAccessService.requireAdmin(organizationId, actorUserId);
        String prefix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String rawKey = "ak_" + prefix + "_" + Hashing.randomUrlToken(32);
        ApiKey apiKey = apiKeyRepository.save(new ApiKey(
                organizationId,
                request.name(),
                prefix,
                Hashing.sha256Hex(rawKey)));
        auditService.record(
                organizationId,
                actorUserId,
                "API_KEY_CREATED",
                "ApiKey",
                apiKey.getId(),
                Map.of("name", request.name(), "keyPrefix", prefix));
        return new CreateApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                rawKey,
                apiKey.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(UUID actorUserId, UUID organizationId) {
        tenantAccessService.requireMember(organizationId, actorUserId);
        return apiKeyRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    @Transactional
    public void revoke(UUID actorUserId, UUID organizationId, UUID keyId) {
        tenantAccessService.requireAdmin(organizationId, actorUserId);
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(key -> key.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        apiKey.revoke();
        auditService.record(
                organizationId,
                actorUserId,
                "API_KEY_REVOKED",
                "ApiKey",
                apiKey.getId(),
                Map.of("keyPrefix", apiKey.getKeyPrefix()));
    }

    @Transactional
    public AuthenticatedApiKey authenticate(String rawApiKey) {
        String prefix = extractPrefix(rawApiKey);
        ApiKey apiKey = apiKeyRepository.findByKeyPrefix(prefix)
                .orElseThrow(this::apiKeyUnauthorized);
        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw apiKeyUnauthorized();
        }
        if (!Hashing.constantTimeEquals(apiKey.getKeyHash(), Hashing.sha256Hex(rawApiKey))) {
            throw apiKeyUnauthorized();
        }
        apiKey.markUsed();
        return new AuthenticatedApiKey(apiKey.getOrganizationId(), apiKey.getId(), apiKey.getKeyPrefix());
    }

    private String extractPrefix(String rawApiKey) {
        String[] parts = rawApiKey.split("_", 3);
        if (parts.length != 3 || !"ak".equals(parts[0]) || parts[1].isBlank()) {
            throw apiKeyUnauthorized();
        }
        return parts[1];
    }

    private ResponseStatusException apiKeyUnauthorized() {
        metricsService.apiKeyAuthFailure();
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
    }
}
