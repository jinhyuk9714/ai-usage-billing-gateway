package io.github.sungjh.aiusagebillinggateway.usage;

import io.github.sungjh.aiusagebillinggateway.security.SecurityPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    UsageEventResponse ingest(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody UsageEventRequest request) {
        UsageEventResponse response = usageService.ingest(
                SecurityPrincipal.currentApiKey(),
                idempotencyKey,
                request);
        if (response.duplicate()) {
            throw new DuplicateUsageResponseException(response);
        }
        return response;
    }
}
