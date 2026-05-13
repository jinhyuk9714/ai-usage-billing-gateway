package io.github.sungjh.aiusagebillinggateway.repository;

import io.github.sungjh.aiusagebillinggateway.domain.Invoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByOrganizationIdAndBillingPeriod(UUID organizationId, String billingPeriod);
}
