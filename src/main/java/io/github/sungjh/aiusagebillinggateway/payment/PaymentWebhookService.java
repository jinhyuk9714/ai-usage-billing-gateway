package io.github.sungjh.aiusagebillinggateway.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sungjh.aiusagebillinggateway.audit.AuditService;
import io.github.sungjh.aiusagebillinggateway.common.Hashing;
import io.github.sungjh.aiusagebillinggateway.domain.Invoice;
import io.github.sungjh.aiusagebillinggateway.domain.Payment;
import io.github.sungjh.aiusagebillinggateway.domain.PaymentStatus;
import io.github.sungjh.aiusagebillinggateway.domain.PaymentWebhookEvent;
import io.github.sungjh.aiusagebillinggateway.ledger.LedgerService;
import io.github.sungjh.aiusagebillinggateway.observability.MetricsService;
import io.github.sungjh.aiusagebillinggateway.repository.InvoiceRepository;
import io.github.sungjh.aiusagebillinggateway.repository.PaymentRepository;
import io.github.sungjh.aiusagebillinggateway.repository.PaymentWebhookEventRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentWebhookService {

    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final LedgerService ledgerService;
    private final AuditService auditService;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public PaymentWebhookService(
            PaymentWebhookEventRepository webhookEventRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            LedgerService ledgerService,
            AuditService auditService,
            MetricsService metricsService,
            ObjectMapper objectMapper,
            @Value("${payment.webhook-secret}") String webhookSecret) {
        this.webhookEventRepository = webhookEventRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public PaymentWebhookResponse process(String signature, String body) {
        verifySignature(signature, body);
        metricsService.webhookReceived();
        PaymentWebhookRequest request = parse(body);
        String payloadHash = Hashing.sha256Hex(body);
        return webhookEventRepository.findByProviderEventId(request.providerEventId())
                .map(existing -> duplicateOrConflict(existing, payloadHash))
                .orElseGet(() -> processNew(request, payloadHash));
    }

    private PaymentWebhookResponse processNew(PaymentWebhookRequest request, String payloadHash) {
        webhookEventRepository.save(new PaymentWebhookEvent(
                request.providerEventId(),
                request.type(),
                payloadHash));
        if (!request.type().equals("payment.succeeded")
                && !request.type().equals("payment.failed")
                && !request.type().equals("payment.refunded")) {
            return new PaymentWebhookResponse(request.providerEventId(), false, "ignored");
        }
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        PaymentStatus paymentStatus = switch (request.type()) {
            case "payment.succeeded" -> PaymentStatus.SUCCEEDED;
            case "payment.failed" -> PaymentStatus.FAILED;
            default -> PaymentStatus.REFUNDED;
        };
        Payment payment = paymentRepository.save(new Payment(
                invoice.getOrganizationId(),
                invoice.getId(),
                request.providerEventId(),
                paymentStatus,
                request.amountMinor(),
                request.currency()));
        if (paymentStatus == PaymentStatus.SUCCEEDED) {
            invoice.markPaid();
            ledgerService.recordPaymentSucceeded(invoice, payment);
        } else if (paymentStatus == PaymentStatus.FAILED) {
            invoice.markPaymentFailed();
        }
        auditService.record(
                invoice.getOrganizationId(),
                null,
                "PAYMENT_WEBHOOK_PROCESSED",
                "PaymentWebhookEvent",
                null,
                Map.of("providerEventId", request.providerEventId(), "type", request.type()));
        return new PaymentWebhookResponse(request.providerEventId(), false, "processed");
    }

    private PaymentWebhookResponse duplicateOrConflict(PaymentWebhookEvent existing, String payloadHash) {
        if (!existing.getPayloadHash().equals(payloadHash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Provider event id was already used with a different payload");
        }
        metricsService.webhookDuplicate();
        return new PaymentWebhookResponse(null, true, "duplicate");
    }

    private void verifySignature(String signature, String body) {
        if (signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook signature required");
        }
        String expected = Hashing.hmacSha256Hex(webhookSecret, body);
        if (!Hashing.constantTimeEquals(expected, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
    }

    private PaymentWebhookRequest parse(String body) {
        try {
            return objectMapper.readValue(body, PaymentWebhookRequest.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload", exception);
        }
    }
}
