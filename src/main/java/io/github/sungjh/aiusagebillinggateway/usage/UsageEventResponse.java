package io.github.sungjh.aiusagebillinggateway.usage;

import java.util.UUID;

public record UsageEventResponse(UUID id, boolean duplicate) {
}
