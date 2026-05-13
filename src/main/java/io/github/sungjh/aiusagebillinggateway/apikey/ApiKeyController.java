package io.github.sungjh.aiusagebillinggateway.apikey;

import io.github.sungjh.aiusagebillinggateway.security.SecurityPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{orgId}/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateApiKeyResponse create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateApiKeyRequest request) {
        return apiKeyService.create(SecurityPrincipal.currentUserId(), orgId, request);
    }

    @GetMapping
    List<ApiKeyResponse> list(@PathVariable UUID orgId) {
        return apiKeyService.list(SecurityPrincipal.currentUserId(), orgId);
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable UUID orgId, @PathVariable UUID keyId) {
        apiKeyService.revoke(SecurityPrincipal.currentUserId(), orgId, keyId);
    }
}
