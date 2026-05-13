package io.github.sungjh.aiusagebillinggateway.usage;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.sungjh.aiusagebillinggateway.domain.UsageMetric;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record UsageEventRequest(
        @NotNull UsageMetric metric,
        @Positive long quantity,
        Instant occurredAt,
        JsonNode metadata) {
}
