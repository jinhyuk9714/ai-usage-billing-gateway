# AGENTS.md

## Project Goal

This repository is a backend portfolio project.

The goal is to build an **AI API Gateway + Multi-tenant Usage Billing Platform** that demonstrates production-style backend engineering:

- multi-tenant data isolation
- API key authentication
- RBAC
- usage metering
- quota enforcement
- idempotent ingestion
- invoice generation
- payment webhook idempotency
- immutable ledger
- audit logging
- reconciliation
- observability
- Testcontainers-based integration tests
- conservative, evidence-based documentation

This project is not a toy CRUD app. Every feature should answer a real backend reliability, security, or consistency problem.

## Core Domain

The system models a SaaS platform where organizations use API keys to call an AI/API gateway.

The backend collects usage events, enforces quota, generates invoices, processes mock payment webhooks, and records financial state changes in an immutable ledger.

Primary domain concepts:

- User
- Organization
- Workspace
- Membership
- Role
- ApiKey
- Plan
- Subscription
- UsageEvent
- Invoice
- InvoiceItem
- Payment
- PaymentWebhookEvent
- LedgerEntry
- AuditLog

## Technology Stack

Use this stack unless explicitly instructed otherwise:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- Testcontainers
- JUnit 5
- Mockito
- AssertJ
- Awaitility if async behavior is needed
- Micrometer + Spring Actuator
- Prometheus-compatible metrics
- Gradle Kotlin DSL
- Docker Compose
- k6 for load scripts

Do not introduce Kafka unless a specific PR explicitly asks for it. Previous portfolio projects already cover Kafka. This project should focus on billing consistency, tenant isolation, webhook idempotency, ledger, and auditability.

## Non-Negotiable Design Principles

### 1. Tenant Isolation

Every organization-scoped table must have `organization_id` directly or through a clearly controlled relationship.

Never allow a user from organization A to access organization B data.

Every service method that reads or mutates organization-scoped data must validate membership or ownership.

Integration tests must cover cross-tenant access denial.

### 2. Idempotency

Any endpoint that can be retried must have an idempotency strategy.

Required idempotent flows:

- usage event ingestion
- payment webhook processing
- invoice generation job
- refund or credit adjustment if implemented
- ledger entry creation

Prefer DB unique constraints over in-memory checks.

### 3. Ledger Integrity

Financial state must not be represented only by mutable status fields.

Use immutable `LedgerEntry` rows for money-affecting events.

Ledger entries must not be updated or deleted after creation.

Invoice/payment/refund flows must be explainable from ledger entries.

### 4. Conservative Claims

Do not write documentation that claims production readiness unless the repo actually implements it.

Do not invent benchmark numbers.

If a k6 script exists but has not been run, document it as:

```text
scenario added, result pending
```

Measured, Verified, and Pending must be clearly separated.

### 5. Test First for Risky Changes

For security, billing, idempotency, ledger, and tenant isolation changes:

1. Add or update a failing test first.
2. Implement the minimal code to pass.
3. Run the focused test.
4. Run the full test suite when practical.

Do not weaken tests to make implementation pass.

### 6. Small PRs

Keep each PR focused.

Recommended PR boundaries:

- project scaffold
- auth + organization + membership
- API key auth + gateway endpoint
- usage ingestion + idempotency
- quota/rate limit
- plan/subscription
- invoice generation
- payment webhook
- ledger
- audit log
- observability
- docs/performance

Do not combine unrelated concerns in one PR unless the user explicitly requests a bundled MVP.

## API and Security Rules

- Public auth endpoints may be unauthenticated.
- Organization, billing, usage, invoice, and admin APIs must be authenticated.
- API gateway calls should authenticate using API keys.
- User-facing APIs should authenticate using JWT.
- Passwords must be hashed.
- API keys must not be stored in plaintext. Store only a hash.
- API key response should show the raw key only once at creation time.
- Logs must not include raw API keys, passwords, JWTs, or payment webhook secrets.

## Database Rules

Use Flyway migrations under:

```text
src/main/resources/db/migration
```

Use versioned SQL migrations and keep JPA DDL generation disabled or validate-only:

```yaml
spring.jpa.hibernate.ddl-auto: validate
spring.sql.init.mode: never
```

Schema changes must be reflected in Flyway migrations and tests.

## Testing Rules

Use Testcontainers for PostgreSQL and Redis integration tests.

At minimum, include tests for:

- organization data isolation
- role-based access control
- API key authentication
- API key cannot access another organization
- duplicate usage event idempotency
- quota enforcement
- invoice generation idempotency
- webhook idempotency
- invalid webhook signature rejection
- ledger balance consistency
- audit log creation for billing-sensitive changes
- metrics endpoint availability if observability is implemented

Prefer focused tests first, then full suite.

## Documentation Rules

Maintain these documents:

```text
README.md
docs/DESIGN.md
docs/PERF_RESULT.md
docs/STUDY_GUIDE.md
```

README must include a one-line summary, core problems, architecture summary, evidence matrix, API summary, how to run, tests, and limitations.

DESIGN.md must explain tenant isolation, API key auth, usage ingestion, idempotency, quota/rate limit, invoice generation, webhook processing, ledger model, audit log, observability, and limitations.

PERF_RESULT.md must separate Measured, Verified, and Pending. Never add fake performance results.

## Observability Rules

Use Spring Actuator and Micrometer. Add metrics only when they are meaningful and low-cardinality.

Good metrics:

- usage events ingested
- duplicate usage events skipped
- quota exceeded count
- invoice generation count
- invoice generation failures
- payment webhook received count
- payment webhook duplicate count
- ledger entries created
- audit logs created
- API key auth failure count

Avoid high-cardinality tags such as raw userId, organizationId, email, API key, invoiceId, or request path with IDs.

## Final Report Format

After each task, report:

1. Summary of changes
2. Files changed
3. Tests added or updated
4. Commands run and results
5. Remaining limitations
6. Whether main was touched
