# ai-usage-billing-gateway

Multi-tenant SaaS backend that demonstrates API key authentication, usage metering, quota/rate limiting, invoice generation, mock payment webhooks, immutable ledger entries, audit logs, and Testcontainers-backed verification.

## Core Problems

- Prevent cross-tenant access to organization-scoped data.
- Store API keys safely by returning the raw key once and persisting only a hash.
- Make retryable usage ingestion, invoice generation, and payment webhook processing idempotent.
- Explain financial state transitions with append-only ledger entries, not only mutable invoice/payment statuses.
- Keep documentation conservative by separating measured, verified, and pending evidence.

## Architecture

- Java 21, Spring Boot 3.5.14, Gradle Kotlin DSL.
- Spring Security with JWT for user APIs and `X-API-Key` for gateway/usage APIs.
- PostgreSQL schema owned by Flyway; Hibernate is set to `validate`.
- Redis fixed-window rate limiting for gateway requests.
- Micrometer counters exposed through Actuator/Prometheus.
- Testcontainers PostgreSQL and Redis integration tests.

## Evidence Matrix

| Area | Status | Evidence |
| --- | --- | --- |
| Context, Flyway, health | Verified | `ApplicationContextIT` |
| JWT auth and RBAC | Verified | `AuthTenantSecurityIT` |
| Cross-tenant denial | Verified | `AuthTenantSecurityIT` |
| API key hash/revoke flow | Verified | `ApiKeyUsageQuotaIT` |
| Usage idempotency | Verified | `ApiKeyUsageQuotaIT` |
| Quota and Redis rate limit | Verified | `ApiKeyUsageQuotaIT` |
| Invoice idempotency | Verified | `BillingPaymentLedgerAuditIT` |
| Webhook signature/idempotency | Verified | `BillingPaymentLedgerAuditIT` |
| Ledger balance | Verified | `BillingPaymentLedgerAuditIT` |
| Audit log secret hygiene | Verified | `BillingPaymentLedgerAuditIT` |
| k6 load scenario | Pending | script added, result pending |
| Production readiness | Pending | not claimed |

## API Summary

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/organizations`
- `GET /api/organizations`
- `GET /api/organizations/{orgId}`
- `POST /api/organizations/{orgId}/members`
- `PUT /api/organizations/{orgId}/subscription`
- `POST /api/organizations/{orgId}/api-keys`
- `GET /api/organizations/{orgId}/api-keys`
- `DELETE /api/organizations/{orgId}/api-keys/{keyId}`
- `POST /v1/gateway/mock-completion`
- `POST /api/usage/events`
- `POST /api/organizations/{orgId}/invoices/generate?period=YYYY-MM`
- `POST /api/webhooks/payments`
- `GET /actuator/health`

## Run Locally

```bash
docker compose up -d
./gradlew bootRun
```

The local database defaults are in `src/main/resources/application.yml`.

## Test

```bash
./gradlew test --no-daemon
./gradlew build --no-daemon
git diff --check
k6 inspect k6/mixed-usage-test.js
```

## Limitations

- The AI provider and payment provider are mocks.
- JWT handling is intentionally small for portfolio scope; it is not a full IAM product.
- The ledger is a simplified balanced-entry model and does not claim accounting compliance.
- Redis rate limiting uses a fixed window, which can allow boundary bursts.
- k6 scenario exists, but no benchmark result is claimed.
