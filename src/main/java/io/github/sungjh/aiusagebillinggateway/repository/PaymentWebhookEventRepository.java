package io.github.sungjh.aiusagebillinggateway.repository;

import io.github.sungjh.aiusagebillinggateway.domain.PaymentWebhookEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    Optional<PaymentWebhookEvent> findByProviderEventId(String providerEventId);

    @Modifying
    @Query(
            value = """
                    insert into payment_webhook_events (
                        id,
                        provider_event_id,
                        event_type,
                        payload_hash,
                        processed_at,
                        duplicate
                    )
                    values (
                        :id,
                        :providerEventId,
                        :eventType,
                        :payloadHash,
                        :processedAt,
                        false
                    )
                    on conflict (provider_event_id) do nothing
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("providerEventId") String providerEventId,
            @Param("eventType") String eventType,
            @Param("payloadHash") String payloadHash,
            @Param("processedAt") Instant processedAt);
}
