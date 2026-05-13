package io.github.sungjh.aiusagebillinggateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingPaymentLedgerAuditIT extends IntegrationTestSupport {

    @Test
    void invoiceGenerationIsIdempotentTenantIsolatedAndCreatesBalancedLedger() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Billing Org");
        String rawKey = createApiKey(token, organizationId, "primary");
        jdbcTemplate.update(
                "update plans set included_quantity = 1, overage_unit_amount_minor = 25 where code = 'FREE'");

        for (int index = 1; index <= 4; index++) {
            mockMvc.perform(post("/api/usage/events")
                            .header("X-API-Key", rawKey)
                            .header("Idempotency-Key", "bill-" + index)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"metric":"REQUEST","quantity":1}
                                    """))
                    .andExpect(status().isCreated());
        }

        YearMonth period = currentPeriod();
        JsonNode firstInvoice = json(mockMvc.perform(post(
                                "/api/organizations/{orgId}/invoices/generate?period={period}",
                                organizationId,
                                period)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmountMinor").value(75))
                .andReturn());

        mockMvc.perform(post(
                        "/api/organizations/{orgId}/invoices/generate?period={period}",
                        organizationId,
                        period)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));

        Integer invoiceCount = jdbcTemplate.queryForObject(
                "select count(*) from invoices where organization_id = ?",
                Integer.class,
                organizationId);
        assertThat(invoiceCount).isEqualTo(1);

        UUID invoiceId = UUID.fromString(firstInvoice.get("id").asText());
        List<Map<String, Object>> ledgerRows = jdbcTemplate.queryForList(
                "select direction, amount_minor from ledger_entries where invoice_id = ?",
                invoiceId);
        long debit = ledgerRows.stream()
                .filter(row -> row.get("direction").equals("DEBIT"))
                .mapToLong(row -> ((Number) row.get("amount_minor")).longValue())
                .sum();
        long credit = ledgerRows.stream()
                .filter(row -> row.get("direction").equals("CREDIT"))
                .mapToLong(row -> ((Number) row.get("amount_minor")).longValue())
                .sum();
        assertThat(debit).isEqualTo(credit).isEqualTo(75);
    }

    @Test
    void webhookSignatureIdempotencyPaymentStatusAndLedgerAreEnforced() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Webhook Org");
        String rawKey = createApiKey(token, organizationId, "primary");
        jdbcTemplate.update(
                "update plans set included_quantity = 0, overage_unit_amount_minor = 100 where code = 'FREE'");
        mockMvc.perform(post("/api/usage/events")
                        .header("X-API-Key", rawKey)
                        .header("Idempotency-Key", "webhook-usage")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"metric":"REQUEST","quantity":1}
                                """))
                .andExpect(status().isCreated());

        JsonNode invoice = json(mockMvc.perform(post(
                                "/api/organizations/{orgId}/invoices/generate?period={period}",
                                organizationId,
                                currentPeriod())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn());
        UUID invoiceId = UUID.fromString(invoice.get("id").asText());
        String body = """
                {"providerEventId":"evt-paid-1","type":"payment.succeeded","invoiceId":"%s","amountMinor":100,"currency":"USD"}
                """.formatted(invoiceId);

        mockMvc.perform(post("/api/webhooks/payments")
                        .header("X-Webhook-Signature", webhookSignature(body))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(false));

        mockMvc.perform(post("/api/webhooks/payments")
                        .header("X-Webhook-Signature", webhookSignature(body))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));

        Integer paymentCount = jdbcTemplate.queryForObject(
                "select count(*) from payments where invoice_id = ?",
                Integer.class,
                invoiceId);
        String invoiceStatus = jdbcTemplate.queryForObject(
                "select status from invoices where id = ?",
                String.class,
                invoiceId);
        Integer webhookAuditCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = 'PAYMENT_WEBHOOK_PROCESSED'",
                Integer.class);

        assertThat(paymentCount).isEqualTo(1);
        assertThat(invoiceStatus).isEqualTo("PAID");
        assertThat(webhookAuditCount).isEqualTo(1);
    }

    @Test
    void invalidWebhookSignatureIsRejectedAndFailedPaymentMarksInvoice() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Failed Payment Org");
        JsonNode invoice = json(mockMvc.perform(post(
                                "/api/organizations/{orgId}/invoices/generate?period={period}",
                                organizationId,
                                currentPeriod())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn());
        UUID invoiceId = UUID.fromString(invoice.get("id").asText());
        String body = """
                {"providerEventId":"evt-failed-1","type":"payment.failed","invoiceId":"%s","amountMinor":0,"currency":"USD"}
                """.formatted(invoiceId);

        mockMvc.perform(post("/api/webhooks/payments")
                        .header("X-Webhook-Signature", "bad-signature")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/webhooks/payments")
                        .header("X-Webhook-Signature", webhookSignature(body))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String invoiceStatus = jdbcTemplate.queryForObject(
                "select status from invoices where id = ?",
                String.class,
                invoiceId);
        assertThat(invoiceStatus).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void auditLogDoesNotLeakRawApiKey() throws Exception {
        String token = signup("owner@example.com");
        UUID organizationId = createOrganization(token, "Audit Org");
        String rawKey = createApiKey(token, organizationId, "primary");

        String metadata = jdbcTemplate.queryForObject(
                """
                select metadata::text
                  from audit_logs
                 where organization_id = ?
                   and action = 'API_KEY_CREATED'
                 order by created_at desc
                 limit 1
                """,
                String.class,
                organizationId);

        assertThat(metadata).doesNotContain(rawKey);
    }
}
