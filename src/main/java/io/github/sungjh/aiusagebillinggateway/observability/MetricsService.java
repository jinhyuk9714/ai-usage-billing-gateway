package io.github.sungjh.aiusagebillinggateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricsService {

    private final Counter usageEventsIngested;
    private final Counter duplicateUsageEvents;
    private final Counter quotaExceeded;
    private final Counter apiKeyAuthFailures;
    private final Counter invoicesGenerated;
    private final Counter invoicesFailed;
    private final Counter paymentWebhooksReceived;
    private final Counter paymentWebhookDuplicates;
    private final Counter ledgerEntriesCreated;
    private final Counter auditLogsCreated;

    public MetricsService(MeterRegistry meterRegistry) {
        this.usageEventsIngested = meterRegistry.counter("usage.events.ingested");
        this.duplicateUsageEvents = meterRegistry.counter("usage.events.duplicates");
        this.quotaExceeded = meterRegistry.counter("quota.exceeded");
        this.apiKeyAuthFailures = meterRegistry.counter("api.key.auth.failures");
        this.invoicesGenerated = meterRegistry.counter("invoices.generated");
        this.invoicesFailed = meterRegistry.counter("invoices.failed");
        this.paymentWebhooksReceived = meterRegistry.counter("payment.webhooks.received");
        this.paymentWebhookDuplicates = meterRegistry.counter("payment.webhooks.duplicates");
        this.ledgerEntriesCreated = meterRegistry.counter("ledger.entries.created");
        this.auditLogsCreated = meterRegistry.counter("audit.logs.created");
    }

    public void usageIngested() {
        usageEventsIngested.increment();
    }

    public void duplicateUsage() {
        duplicateUsageEvents.increment();
    }

    public void quotaExceeded() {
        quotaExceeded.increment();
    }

    public void apiKeyAuthFailure() {
        apiKeyAuthFailures.increment();
    }

    public void invoiceGenerated() {
        invoicesGenerated.increment();
    }

    public void invoiceFailed() {
        invoicesFailed.increment();
    }

    public void webhookReceived() {
        paymentWebhooksReceived.increment();
    }

    public void webhookDuplicate() {
        paymentWebhookDuplicates.increment();
    }

    public void ledgerEntryCreated() {
        ledgerEntriesCreated.increment();
    }

    public void auditLogCreated() {
        auditLogsCreated.increment();
    }
}
