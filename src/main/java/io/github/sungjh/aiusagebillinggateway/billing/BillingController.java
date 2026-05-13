package io.github.sungjh.aiusagebillinggateway.billing;

import io.github.sungjh.aiusagebillinggateway.security.SecurityPrincipal;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{orgId}/invoices")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    InvoiceResponse generate(@PathVariable UUID orgId, @RequestParam String period) {
        InvoiceResponse response = billingService.generate(
                SecurityPrincipal.currentUserId(),
                orgId,
                YearMonth.parse(period));
        if (response.duplicate()) {
            throw new DuplicateInvoiceResponseException(response);
        }
        return response;
    }
}
