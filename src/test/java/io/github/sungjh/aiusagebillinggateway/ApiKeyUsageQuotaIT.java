package io.github.sungjh.aiusagebillinggateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiKeyUsageQuotaIT extends IntegrationTestSupport {

    @Test
    void apiKeyRawValueIsShownOnceAndNotStored() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Keys Org");
        String rawKey = createApiKey(token, organizationId, "primary");

        assertThat(rawKey).startsWith("ak_");
        Integer plaintextMatches = jdbcTemplate.queryForObject(
                "select count(*) from api_keys where key_hash = ? or key_prefix = ?",
                Integer.class,
                rawKey,
                rawKey);
        assertThat(plaintextMatches).isZero();

        mockMvc.perform(get("/api/organizations/{orgId}/api-keys", organizationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rawApiKey").doesNotExist())
                .andExpect(jsonPath("$[0].keyPrefix").exists());
    }

    @Test
    void validApiKeyCanCallGatewayAndRevokedKeyIsRejected() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Gateway Org");
        String rawKey = createApiKey(token, organizationId, "primary");
        UUID keyId = jdbcTemplate.queryForObject(
                "select id from api_keys where organization_id = ?",
                UUID.class,
                organizationId);

        mockMvc.perform(post("/v1/gateway/mock-completion")
                        .header("X-API-Key", rawKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"prompt":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"));

        mockMvc.perform(delete("/api/organizations/{orgId}/api-keys/{keyId}", organizationId, keyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/gateway/mock-completion")
                        .header("X-API-Key", rawKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"prompt":"hello again"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usageEventIngestionIsIdempotentAndRejectsPayloadMismatch() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Usage Org");
        String rawKey = createApiKey(token, organizationId, "primary");

        String payload = """
                {"metric":"REQUEST","quantity":1,"metadata":{"route":"mock"}}
                """;
        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "usage-1")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duplicate").value(false));

        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "usage-1")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from usage_events where organization_id = ?",
                Integer.class,
                organizationId);
        assertThat(count).isEqualTo(1);

        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "usage-1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"metric":"REQUEST","quantity":2,"metadata":{"route":"mock"}}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void usageIdempotencyRejectsSameKeyWithDifferentOccurredAt() throws Exception {
        String token = signup("owner-occurred@example.com");
        UUID organizationId = createOrganization(token, "Usage Occurred Org");
        String rawKey = createApiKey(token, organizationId, "primary");

        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "usage-occurred-at")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "metric":"REQUEST",
                                  "quantity":1,
                                  "occurredAt":"2026-05-01T00:00:00Z",
                                  "metadata":{"route":"mock"}
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "usage-occurred-at")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "metric":"REQUEST",
                                  "quantity":1,
                                  "occurredAt":"2026-05-02T00:00:00Z",
                                  "metadata":{"route":"mock"}
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidUsageQuantityIsRejected() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Invalid Usage Org");
        String rawKey = createApiKey(token, organizationId, "primary");

        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "invalid-quantity")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"metric":"REQUEST","quantity":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quotaAndRateLimitAreEnforcedPerTenant() throws Exception {
        String ownerToken = signup("owner@example.com");
        UUID organizationId = createOrganization(ownerToken, "Quota Org");
        String rawKey = createApiKey(ownerToken, organizationId, "primary");
        jdbcTemplate.update("update plans set included_quantity = 1 where code = 'FREE'");

        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "quota-1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"metric":"REQUEST","quantity":1}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/gateway/mock-completion")
                        .header("X-API-Key", rawKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"prompt":"blocked by quota"}
                                """))
                .andExpect(status().isTooManyRequests());

        jdbcTemplate.update("update plans set included_quantity = 10000 where code = 'FREE'");
        String secondToken = signup("second@example.com");
        UUID secondOrganizationId = createOrganization(secondToken, "Rate Org");
        String secondRawKey = createApiKey(secondToken, secondOrganizationId, "primary");

        mockMvc.perform(post("/v1/gateway/mock-completion")
                        .header("X-API-Key", secondRawKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"prompt":"first"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/gateway/mock-completion")
                        .header("X-API-Key", secondRawKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"prompt":"second"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/gateway/mock-completion")
                        .header("X-API-Key", secondRawKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"prompt":"third"}
                                """))
                .andExpect(status().isTooManyRequests());

        String thirdToken = signup("third@example.com");
        UUID thirdOrganizationId = createOrganization(thirdToken, "Independent Rate Org");
        String thirdRawKey = createApiKey(thirdToken, thirdOrganizationId, "primary");
        mockMvc.perform(post("/v1/gateway/mock-completion")
                        .header("X-API-Key", thirdRawKey)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"prompt":"independent"}
                                """))
                .andExpect(status().isOk());
    }
}
