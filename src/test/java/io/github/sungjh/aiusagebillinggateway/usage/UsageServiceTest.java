package io.github.sungjh.aiusagebillinggateway.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sungjh.aiusagebillinggateway.domain.UsageMetric;
import io.github.sungjh.aiusagebillinggateway.observability.MetricsService;
import io.github.sungjh.aiusagebillinggateway.repository.UsageEventRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsageServiceTest {

    @Test
    void requestHashIncludesOccurredAt() throws Exception {
        UsageService usageService = new UsageService(
                mock(UsageEventRepository.class),
                new ObjectMapper().findAndRegisterModules(),
                mock(MetricsService.class));
        UsageEventRequest first = new UsageEventRequest(
                UsageMetric.REQUEST,
                1,
                Instant.parse("2026-05-01T00:00:00Z"),
                null);
        UsageEventRequest second = new UsageEventRequest(
                UsageMetric.REQUEST,
                1,
                Instant.parse("2026-05-02T00:00:00Z"),
                null);

        assertThat(requestHash(usageService, first))
                .isNotEqualTo(requestHash(usageService, second));
    }

    private String requestHash(UsageService usageService, UsageEventRequest request) throws Exception {
        Method method = UsageService.class.getDeclaredMethod("requestHash", UsageEventRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(usageService, request);
    }
}
