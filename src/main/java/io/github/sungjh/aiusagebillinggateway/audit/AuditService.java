package io.github.sungjh.aiusagebillinggateway.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sungjh.aiusagebillinggateway.domain.AuditLog;
import io.github.sungjh.aiusagebillinggateway.observability.MetricsService;
import io.github.sungjh.aiusagebillinggateway.repository.AuditLogRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public AuditService(
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            MetricsService metricsService) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    public void record(
            UUID organizationId,
            UUID actorUserId,
            String action,
            String targetType,
            UUID targetId,
            Map<String, ?> metadata) {
        try {
            String safeMetadata = objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
            auditLogRepository.save(new AuditLog(
                    organizationId,
                    actorUserId,
                    action,
                    targetType,
                    targetId,
                    safeMetadata));
            metricsService.auditLogCreated();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write audit log", exception);
        }
    }
}
