package io.github.sungjh.aiusagebillinggateway.ledger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.sungjh.aiusagebillinggateway.audit.AuditService;
import io.github.sungjh.aiusagebillinggateway.domain.LedgerDirection;
import io.github.sungjh.aiusagebillinggateway.domain.LedgerEntry;
import io.github.sungjh.aiusagebillinggateway.observability.MetricsService;
import io.github.sungjh.aiusagebillinggateway.repository.LedgerEntryRepository;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LedgerServiceTest {

    @Test
    void appendBalancedRejectsUnbalancedEntries() throws Exception {
        LedgerService ledgerService = newLedgerService();
        UUID organizationId = UUID.randomUUID();

        assertThatThrownBy(() -> invokeAppendBalanced(ledgerService, List.of(
                entry(organizationId, LedgerDirection.DEBIT, 100, "USD", "unbalanced:debit"),
                entry(organizationId, LedgerDirection.CREDIT, 90, "USD", "unbalanced:credit"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unbalanced ledger entries");
    }

    @Test
    void appendBalancedRejectsMixedCurrencies() throws Exception {
        LedgerService ledgerService = newLedgerService();
        UUID organizationId = UUID.randomUUID();

        assertThatThrownBy(() -> invokeAppendBalanced(ledgerService, List.of(
                entry(organizationId, LedgerDirection.DEBIT, 100, "USD", "mixed:debit"),
                entry(organizationId, LedgerDirection.CREDIT, 100, "KRW", "mixed:credit"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("single currency");
    }

    @Test
    void appendBalancedRejectsNonPositiveAmounts() throws Exception {
        LedgerService ledgerService = newLedgerService();
        UUID organizationId = UUID.randomUUID();

        assertThatThrownBy(() -> invokeAppendBalanced(ledgerService, List.of(
                entry(organizationId, LedgerDirection.DEBIT, 0, "USD", "zero:debit"),
                entry(organizationId, LedgerDirection.CREDIT, 0, "USD", "zero:credit"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("positive");
    }

    private LedgerService newLedgerService() {
        return new LedgerService(
                mock(LedgerEntryRepository.class),
                mock(AuditService.class),
                mock(MetricsService.class));
    }

    private void invokeAppendBalanced(
            LedgerService ledgerService,
            List<LedgerEntry> entries) throws Exception {
        Method method = LedgerService.class.getDeclaredMethod("appendBalanced", List.class);
        method.setAccessible(true);
        try {
            method.invoke(ledgerService, entries);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            throw exception;
        }
    }

    private LedgerEntry entry(
            UUID organizationId,
            LedgerDirection direction,
            long amountMinor,
            String currency,
            String idempotencyKey) {
        return new LedgerEntry(
                organizationId,
                UUID.randomUUID(),
                null,
                "test-group",
                "TEST",
                "TEST_ACCOUNT",
                direction,
                amountMinor,
                currency,
                idempotencyKey);
    }
}
