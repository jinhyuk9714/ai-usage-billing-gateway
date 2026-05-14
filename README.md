# AI Usage Billing Gateway

![CI](https://github.com/jinhyuk9714/ai-usage-billing-gateway/actions/workflows/ci.yml/badge.svg)

멀티테넌트 SaaS 환경에서 **API Key 인증, 사용량 수집, quota/rate limit, invoice 생성, payment webhook, append-only ledger, audit log**를 검증한 Spring Boot 백엔드 프로젝트입니다.

이 프로젝트는 단순 결제 CRUD가 아니라, SaaS 과금 시스템에서 쉽게 깨질 수 있는 **tenant isolation, retry idempotency, webhook duplicate delivery, ledger consistency, audit secret hygiene**를 코드와 테스트로 검증하는 데 초점을 둡니다.

---

## 핵심 문제

| 문제 | 구현한 대응 |
| --- | --- |
| 다른 organization의 billing/usage 데이터에 접근할 수 있음 | organization membership 기반 `TenantAccessService`로 모든 organization-scoped API 접근 제어 |
| API Key 원문이 DB에 저장되면 유출 시 즉시 악용 가능 | raw API key는 생성 시 1회만 반환하고, DB에는 `keyPrefix`와 hash만 저장 |
| 사용량 이벤트가 재시도되면 중복 과금될 수 있음 | `organizationId + Idempotency-Key` unique scope와 request hash 비교 |
| 같은 idempotency key로 다른 payload가 들어올 수 있음 | 기존 request hash와 다르면 `409 Conflict` |
| invoice generation job이 두 번 실행될 수 있음 | `organizationId + billingPeriod` 기준 idempotent invoice generation |
| payment webhook은 중복 delivery될 수 있음 | `providerEventId` reserve와 payload hash 비교로 duplicate/conflict 처리 |
| 금액 상태를 status update만으로 설명하기 어려움 | invoice/payment 흐름을 append-only ledger entry로 기록 |
| audit log에 secret이 남을 수 있음 | raw API key 대신 key prefix 등 안전한 metadata만 기록 |
| API 남용을 막아야 함 | Redis fixed-window rate limit과 plan quota check |
| 문서가 실제 구현보다 과장될 수 있음 | `Measured / Verified / Pending`을 분리해 보수적으로 문서화 |

---

## 아키텍처 요약

```text
User APIs
  └─ JWT Authentication
      └─ Organization / Membership / RBAC
          ├─ API Key Management
          ├─ Subscription / Plan
          ├─ Invoice Generation
          ├─ Ledger
          └─ Audit Log

Gateway / Usage APIs
  └─ X-API-Key Authentication
      ├─ Redis Rate Limit
      ├─ Monthly Quota Check
      ├─ Mock AI Gateway
      └─ Usage Event Ingestion

Payment Provider Mock
  └─ Webhook Signature Verification
      ├─ Webhook Event Idempotency
      ├─ Payment Status Update
      ├─ Ledger Entry Creation
      └─ Audit Log
```

---

## 주요 흐름

### 1. API Key 발급과 Gateway 호출

```text
User signup/login
→ organization 생성
→ API key 생성
  - raw key는 응답으로 1회만 반환
  - DB에는 key prefix와 hash만 저장
→ X-API-Key로 /v1/gateway/mock-completion 호출
→ quota/rate limit 검사
→ mock AI response 반환
→ usage event 기록
```

API Key는 다음 형태로 생성됩니다.

```text
ak_<prefix>_<secret>
```

DB에는 raw key를 저장하지 않습니다.

```text
key_prefix = <prefix>
key_hash   = sha256(rawApiKey)
```

---

### 2. Usage Event Idempotency

명시적 사용량 적재 API는 `Idempotency-Key`를 요구합니다.

```http
POST /api/usage/events
X-API-Key: ak_xxxxxxxx_secret
Idempotency-Key: usage-2026-05-14-001
```

```json
{
  "metric": "REQUEST",
  "quantity": 1,
  "occurredAt": "2026-05-14T00:00:00Z",
  "metadata": {
    "route": "mock-completion"
  }
}
```

idempotency 판단에는 다음 값이 포함됩니다.

```text
metric
quantity
occurredAt
metadata
```

처리 정책:

| 상황 | 결과 |
| --- | --- |
| 같은 organization + 같은 idempotency key + 같은 payload | 기존 usage event 반환, duplicate=true |
| 같은 organization + 같은 idempotency key + 다른 payload | 409 Conflict |
| 다른 organization의 같은 idempotency key | 독립적으로 처리 |

> 이 프로젝트는 DB unique constraint 기반 idempotent storage를 구현합니다. 분산 시스템 전체의 exactly-once 처리를 주장하지 않습니다.

---

### 3. Quota와 Rate Limit

Gateway 호출은 두 단계를 통과해야 합니다.

```text
1. Monthly quota check
2. Redis fixed-window rate limit
```

요금제 예시:

| Plan | Included Requests | Overage |
| --- | ---: | --- |
| FREE | 10,000 / month | 허용하지 않음 |
| PRO | 100,000 / month | 허용 |
| BUSINESS | 1,000,000 / month | 허용 |

Redis rate limit은 API key 단위 fixed-window 방식입니다.

```text
rate:api-key:{apiKeyId}:{epochMinute}
```

Redis 장애 시에는 fail-closed 정책을 사용합니다.

```text
Rate limiter unavailable → 503 Service Unavailable
```

---

### 4. Invoice Generation

월별 invoice는 organization과 billing period 기준으로 idempotent하게 생성됩니다.

```http
POST /api/organizations/{orgId}/invoices/generate?period=2026-05
Authorization: Bearer <JWT>
```

계산 흐름:

```text
usage_events
→ monthly usage sum
→ subscription plan lookup
→ included quota 적용
→ overage 계산
→ invoice 생성
→ invoice_items 생성
→ ledger entries 생성
→ audit log 기록
```

중복 실행 정책:

| 상황 | 결과 |
| --- | --- |
| 같은 organization + 같은 billing period 최초 실행 | invoice 생성 |
| 같은 organization + 같은 billing period 재실행 | 기존 invoice 반환, duplicate=true |

---

### 5. Payment Webhook

Mock payment provider webhook은 HMAC-SHA256 signature를 검증합니다.

```http
POST /api/webhooks/payments
X-Webhook-Signature: <hmac>
```

지원하는 event type:

```text
payment.succeeded
payment.failed
payment.refunded
```

Webhook idempotency 정책:

| 상황 | 결과 |
| --- | --- |
| 새로운 providerEventId | webhook event reserve 후 처리 |
| 같은 providerEventId + 같은 payload | duplicate=true |
| 같은 providerEventId + 다른 payload | 409 Conflict |

동시 중복 delivery를 고려해 `providerEventId`를 먼저 reserve합니다.

```sql
INSERT ... ON CONFLICT (provider_event_id) DO NOTHING
```

---

### 6. Ledger

금액 상태는 invoice/payment status만으로 설명하지 않고, ledger entry로 남깁니다.

Invoice 발행 예시:

```text
DEBIT   ACCOUNTS_RECEIVABLE   75 USD
CREDIT  REVENUE               75 USD
```

Payment 성공 예시:

```text
DEBIT   CASH                  75 USD
CREDIT  ACCOUNTS_RECEIVABLE  75 USD
```

Ledger entry group은 service layer에서 다음 invariant를 검증합니다.

```text
debit sum == credit sum
single currency
positive amount only
```

> 이 프로젝트의 ledger는 포트폴리오용 simplified balanced-entry model입니다. 회계 기준 준수나 실제 정산 시스템 수준의 accounting compliance를 주장하지 않습니다.

---

### 7. Audit Log

보안/과금 민감 이벤트는 audit log로 기록합니다.

기록 대상 예시:

```text
ORGANIZATION_CREATED
MEMBER_ADDED
API_KEY_CREATED
API_KEY_REVOKED
SUBSCRIPTION_CHANGED
INVOICE_GENERATED
PAYMENT_WEBHOOK_PROCESSED
LEDGER_ENTRY_GROUP_CREATED
```

API key 관련 audit metadata에는 raw key를 저장하지 않고 key prefix 등 안전한 값만 기록합니다.

---

## 검증한 항목

| 영역 | 상태 | Evidence |
| --- | --- | --- |
| Spring context / Flyway / health endpoint | Verified | `ApplicationContextIT` |
| JWT signup/login | Verified | `AuthTenantSecurityIT` |
| RBAC | Verified | `AuthTenantSecurityIT` |
| Cross-tenant access denial | Verified | `AuthTenantSecurityIT` |
| API key raw value one-time display | Verified | `ApiKeyUsageQuotaIT` |
| API key hash storage | Verified | `ApiKeyUsageQuotaIT` |
| API key revoke | Verified | `ApiKeyUsageQuotaIT` |
| Usage event idempotency | Verified | `ApiKeyUsageQuotaIT`, `UsageServiceTest` |
| Same idempotency key with different payload conflict | Verified | `ApiKeyUsageQuotaIT` |
| `occurredAt` included in usage request hash | Verified | `UsageServiceTest` |
| Quota enforcement | Verified | `ApiKeyUsageQuotaIT` |
| Redis rate limit | Verified | `ApiKeyUsageQuotaIT` |
| Invoice generation idempotency | Verified | `BillingPaymentLedgerAuditIT` |
| Payment webhook signature validation | Verified | `BillingPaymentLedgerAuditIT` |
| Payment webhook duplicate handling | Verified | `BillingPaymentLedgerAuditIT`, `PaymentWebhookServiceTest` |
| Webhook duplicate race fallback | Verified | `PaymentWebhookServiceTest` |
| Ledger debit/credit balance | Verified | `BillingPaymentLedgerAuditIT`, `LedgerServiceTest` |
| Ledger single currency / positive amount invariant | Verified | `LedgerServiceTest` |
| Audit log secret hygiene | Verified | `BillingPaymentLedgerAuditIT` |
| k6 mixed usage scenario | Pending | script added, result pending |
| Production readiness | Pending | not claimed |

---

## API 요약

### Auth

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/auth/signup` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인, JWT 발급 |

### Organization

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/organizations` | organization 생성 |
| `GET` | `/api/organizations` | 내 organization 목록 |
| `GET` | `/api/organizations/{orgId}` | organization 조회 |
| `POST` | `/api/organizations/{orgId}/members` | member 추가 |
| `PUT` | `/api/organizations/{orgId}/subscription` | subscription plan 변경 |

### API Key

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/organizations/{orgId}/api-keys` | API key 생성, raw key 1회 반환 |
| `GET` | `/api/organizations/{orgId}/api-keys` | API key 목록 조회, raw key 미포함 |
| `DELETE` | `/api/organizations/{orgId}/api-keys/{keyId}` | API key revoke |

### Gateway / Usage

| Method | Path | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/v1/gateway/mock-completion` | `X-API-Key` | mock AI gateway 호출 |
| `POST` | `/api/usage/events` | `X-API-Key` | 명시적 usage event 적재 |

### Billing / Payment

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/organizations/{orgId}/invoices/generate?period=YYYY-MM` | invoice 생성 |
| `POST` | `/api/webhooks/payments` | mock payment webhook 수신 |

### Actuator

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/actuator/health` | health check |

> Micrometer metrics는 등록되어 있습니다. 다만 Prometheus scraping을 운영 수준으로 노출하려면 별도 인증/네트워크 정책 구성이 필요합니다.

---

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Gradle Kotlin DSL |
| Database | PostgreSQL |
| Migration | Flyway |
| Cache / Rate Limit | Redis |
| Security | Spring Security, JWT, API Key |
| Persistence | Spring Data JPA, Hibernate validate |
| Observability | Spring Boot Actuator, Micrometer, Prometheus registry |
| Test | JUnit 5, Spring Security Test, Testcontainers, AssertJ, Mockito |
| Load Script | k6 |
| Infra | Docker Compose |
| CI | GitHub Actions |

---

## 실행 방법

### 1. 인프라 실행

```bash
docker compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 로컬 설정은 다음 파일에 있습니다.

```text
src/main/resources/application.yml
```

### 3. 테스트 실행

Testcontainers 기반 테스트는 Docker가 필요합니다.

```bash
./gradlew test --no-daemon
./gradlew build --no-daemon
```

### 4. k6 script inspect

```bash
k6 inspect k6/mixed-usage-test.js
```

### 5. k6 실행

아직 실제 benchmark 결과는 기록하지 않았습니다. 실행 후 환경과 결과를 `docs/PERF_RESULT.md`에 기록해야 합니다.

```bash
BASE_URL=http://localhost:8080 \
API_KEY=<created-api-key> \
k6 run k6/mixed-usage-test.js
```

---

## 성능 결과

현재 실제 부하 테스트 결과는 기록하지 않았습니다.

| 항목 | 상태 |
| --- | --- |
| k6 mixed usage scenario | script added |
| Throughput / latency / error rate | Pending |
| Production performance claim | Not claimed |

자세한 내용은 [docs/PERF_RESULT.md](docs/PERF_RESULT.md)를 참고하세요.

성능 수치를 추가할 때는 반드시 아래 정보를 함께 기록합니다.

```text
- 실행 날짜
- hardware
- JVM version
- PostgreSQL/Redis 실행 환경
- dataset size
- command
- throughput
- p95 / p99 latency
- error rate
```

---

## 한계

| 항목 | 현재 한계 |
| --- | --- |
| AI provider | 실제 AI provider 호출이 아니라 mock response입니다. |
| Payment provider | 실제 PG 연동이 아니라 mock webhook입니다. |
| JWT | refresh token, OAuth, SSO, full IAM lifecycle은 구현하지 않았습니다. |
| Gateway idempotency | 명시적 `/api/usage/events`는 idempotent하지만, `/v1/gateway/mock-completion` 자체의 retry idempotency는 아직 구현하지 않았습니다. |
| Quota concurrency | 현재 quota check는 DB usage sum 기반입니다. 동시 요청에서 strict quota reservation을 보장한다고 주장하지 않습니다. |
| Rate limit | Redis fixed-window 방식이라 window boundary burst 가능성이 있습니다. |
| Invoice scheduler | background scheduler가 아니라 수동 invoice generation endpoint입니다. |
| Refund accounting | `payment.refunded` event type은 받지만, refund reversal ledger는 추가 개선 과제입니다. |
| Ledger | simplified balanced-entry model입니다. 회계 기준 준수나 실제 정산 compliance를 주장하지 않습니다. |
| Audit sanitizer | 현재 caller가 안전한 metadata를 넘긴다는 전제입니다. key 기반 자동 sanitizer는 추가 개선 과제입니다. |
| Observability | metric counter 등록 수준입니다. alerting, dashboard, tracing, SLO 운영 체계는 별도 과제입니다. |
| Performance | k6 scenario는 있지만 실제 benchmark 결과는 pending입니다. |

---

## 문서

| 문서 | 내용 |
| --- | --- |
| [docs/DESIGN.md](docs/DESIGN.md) | tenant isolation, API key auth, usage idempotency, quota/rate limit, invoice, webhook, ledger, audit, observability 설계 |
| [docs/PERF_RESULT.md](docs/PERF_RESULT.md) | measured / verified / pending 성능 문서 |
| [docs/STUDY_GUIDE.md](docs/STUDY_GUIDE.md) | 면접 대비 설명 포인트와 안전한 주장 |

---

## 면접에서 설명할 핵심 포인트

```text
이 프로젝트는 멀티테넌트 SaaS 과금 시스템에서 중요한 보안/정합성 문제를 다룹니다.

organization membership 기반으로 tenant isolation을 검증했고,
API key는 raw key를 한 번만 반환하고 hash만 저장했습니다.

usage event는 organizationId + idempotencyKey unique scope와 request hash로
중복 적재와 payload mismatch를 방어했습니다.

invoice generation은 organizationId + billingPeriod 기준으로 idempotent하게 만들었고,
payment webhook은 providerEventId와 payloadHash로 duplicate/conflict를 구분했습니다.

금액 변화는 invoice/payment status만으로 설명하지 않고,
append-only ledger entry로 기록하며 debit/credit balance, 단일 currency, 양수 amount invariant를 검증했습니다.

성능 수치는 아직 기록하지 않았고,
k6 scenario만 추가된 pending 상태로 문서화했습니다.
```
