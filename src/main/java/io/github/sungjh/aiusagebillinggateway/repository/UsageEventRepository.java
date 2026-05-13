package io.github.sungjh.aiusagebillinggateway.repository;

import io.github.sungjh.aiusagebillinggateway.domain.UsageEvent;
import io.github.sungjh.aiusagebillinggateway.domain.UsageMetric;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {

    Optional<UsageEvent> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("""
            select coalesce(sum(u.quantity), 0)
              from UsageEvent u
             where u.organizationId = :organizationId
               and u.metric = :metric
               and u.occurredAt >= :from
               and u.occurredAt < :to
            """)
    long sumQuantity(
            @Param("organizationId") UUID organizationId,
            @Param("metric") UsageMetric metric,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
