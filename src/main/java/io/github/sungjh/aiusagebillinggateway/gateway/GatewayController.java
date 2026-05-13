package io.github.sungjh.aiusagebillinggateway.gateway;

import io.github.sungjh.aiusagebillinggateway.quota.QuotaService;
import io.github.sungjh.aiusagebillinggateway.security.AuthenticatedApiKey;
import io.github.sungjh.aiusagebillinggateway.security.SecurityPrincipal;
import io.github.sungjh.aiusagebillinggateway.usage.UsageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/gateway")
public class GatewayController {

    private final QuotaService quotaService;
    private final UsageService usageService;

    public GatewayController(QuotaService quotaService, UsageService usageService) {
        this.quotaService = quotaService;
        this.usageService = usageService;
    }

    @PostMapping("/mock-completion")
    Map<String, Object> mockCompletion(@Valid @RequestBody GatewayRequest request) {
        AuthenticatedApiKey apiKey = SecurityPrincipal.currentApiKey();
        quotaService.checkAllowed(apiKey.organizationId(), apiKey.apiKeyId());
        usageService.recordGatewayUsage(apiKey, request.prompt());
        return Map.of(
                "provider", "mock",
                "model", "mock-completion-v1",
                "completion", "Mock response for portfolio verification");
    }

    public record GatewayRequest(@NotBlank String prompt) {
    }
}
