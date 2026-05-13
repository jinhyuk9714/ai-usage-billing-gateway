package io.github.sungjh.aiusagebillinggateway.quota;

import io.github.sungjh.aiusagebillinggateway.domain.Plan;
import io.github.sungjh.aiusagebillinggateway.domain.Subscription;
import io.github.sungjh.aiusagebillinggateway.domain.UsageMetric;
import io.github.sungjh.aiusagebillinggateway.observability.MetricsService;
import io.github.sungjh.aiusagebillinggateway.repository.PlanRepository;
import io.github.sungjh.aiusagebillinggateway.repository.SubscriptionRepository;
import io.github.sungjh.aiusagebillinggateway.repository.UsageEventRepository;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuotaService {

    private final UsageEventRepository usageEventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StringRedisTemplate redisTemplate;
    private final MetricsService metricsService;
    private final long rateLimitPerMinute;

    public QuotaService(
            UsageEventRepository usageEventRepository,
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            StringRedisTemplate redisTemplate,
            MetricsService metricsService,
            @Value("${gateway.rate-limit-per-minute}") long rateLimitPerMinute) {
        this.usageEventRepository = usageEventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public void checkAllowed(UUID organizationId, UUID apiKeyId) {
        checkMonthlyQuota(organizationId);
        checkRateLimit(apiKeyId);
    }

    private void checkMonthlyQuota(UUID organizationId) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Subscription missing"));
        Plan plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Plan missing"));
        YearMonth period = YearMonth.now(ZoneOffset.UTC);
        long used = usageEventRepository.sumQuantity(
                organizationId,
                UsageMetric.REQUEST,
                period.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                period.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        if (!plan.isOverageAllowed() && used >= plan.getIncludedQuantity()) {
            metricsService.quotaExceeded();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Monthly quota exceeded");
        }
    }

    private void checkRateLimit(UUID apiKeyId) {
        try {
            long window = java.time.Instant.now().getEpochSecond() / 60;
            String key = "rate:api-key:" + apiKeyId + ":" + window;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(2));
            }
            if (count != null && count > rateLimitPerMinute) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            }
        } catch (RedisConnectionFailureException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Rate limiter unavailable");
        }
    }
}
