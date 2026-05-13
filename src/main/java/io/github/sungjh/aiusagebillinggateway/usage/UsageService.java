package io.github.sungjh.aiusagebillinggateway.usage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sungjh.aiusagebillinggateway.common.Hashing;
import io.github.sungjh.aiusagebillinggateway.domain.UsageEvent;
import io.github.sungjh.aiusagebillinggateway.domain.UsageMetric;
import io.github.sungjh.aiusagebillinggateway.observability.MetricsService;
import io.github.sungjh.aiusagebillinggateway.repository.UsageEventRepository;
import io.github.sungjh.aiusagebillinggateway.security.AuthenticatedApiKey;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsageService {

    private final UsageEventRepository usageEventRepository;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public UsageService(
            UsageEventRepository usageEventRepository,
            ObjectMapper objectMapper,
            MetricsService metricsService) {
        this.usageEventRepository = usageEventRepository;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    @Transactional
    public UsageEventResponse ingest(
            AuthenticatedApiKey apiKey,
            String idempotencyKey,
            UsageEventRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
        if (request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usage quantity must be positive");
        }
        String requestHash = requestHash(request);
        return usageEventRepository
                .findByOrganizationIdAndIdempotencyKey(apiKey.organizationId(), idempotencyKey)
                .map(existing -> duplicateOrConflict(existing, requestHash))
                .orElseGet(() -> create(apiKey, idempotencyKey, requestHash, request));
    }

    @Transactional
    public void recordGatewayUsage(AuthenticatedApiKey apiKey, String prompt) {
        UsageEventRequest request = new UsageEventRequest(
                UsageMetric.REQUEST,
                1,
                null,
                objectMapper.valueToTree(Map.of("source", "gateway", "promptHash", Hashing.sha256Hex(prompt))));
        create(apiKey, "gateway-" + UUID.randomUUID(), requestHash(request), request);
    }

    private UsageEventResponse duplicateOrConflict(UsageEvent existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was already used with a different payload");
        }
        metricsService.duplicateUsage();
        return new UsageEventResponse(existing.getId(), true);
    }

    private UsageEventResponse create(
            AuthenticatedApiKey apiKey,
            String idempotencyKey,
            String requestHash,
            UsageEventRequest request) {
        try {
            UsageEvent event = usageEventRepository.save(new UsageEvent(
                    apiKey.organizationId(),
                    apiKey.apiKeyId(),
                    idempotencyKey,
                    requestHash,
                    request.metric(),
                    request.quantity(),
                    request.occurredAt() == null ? Instant.now() : request.occurredAt(),
                    metadataString(request.metadata())));
            metricsService.usageIngested();
            return new UsageEventResponse(event.getId(), false);
        } catch (DataIntegrityViolationException exception) {
            return usageEventRepository
                    .findByOrganizationIdAndIdempotencyKey(apiKey.organizationId(), idempotencyKey)
                    .map(existing -> duplicateOrConflict(existing, requestHash))
                    .orElseThrow(() -> exception);
        }
    }

    private String requestHash(UsageEventRequest request) {
        try {
            return Hashing.sha256Hex(objectMapper.writeValueAsString(Map.of(
                    "metric", request.metric(),
                    "quantity", request.quantity(),
                    "metadata", metadataString(request.metadata()))));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash usage event request", exception);
        }
    }

    private String metadataString(JsonNode metadata) {
        try {
            return metadata == null || metadata.isNull() ? "{}" : objectMapper.writeValueAsString(metadata);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid metadata", exception);
        }
    }
}
