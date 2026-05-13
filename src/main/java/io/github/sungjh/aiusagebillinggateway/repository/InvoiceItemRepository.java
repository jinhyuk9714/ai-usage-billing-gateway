package io.github.sungjh.aiusagebillinggateway.repository;

import io.github.sungjh.aiusagebillinggateway.domain.InvoiceItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
}
