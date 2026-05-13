# Design

## Tenant Isolation

Organization-scoped APIs validate membership through `TenantAccessService`. OWNER and ADMIN can perform billing-sensitive operations, while MEMBER can read organization data but cannot change billing/subscription state.

Organization-scoped tables carry `organization_id` directly where practical. Tests cover cross-tenant denial and MEMBER rejection for admin-only subscription changes.

## Authentication

User-facing APIs use JWT bearer tokens. Gateway and usage APIs use `X-API-Key`.

API keys are generated as `ak_<prefix>_<secret>`. The raw key is returned only at creation time. The database stores `key_prefix` and `key_hash`, never the raw key. Revoked keys are rejected by the API key authentication filter.

## Usage Ingestion And Idempotency

`POST /api/usage/events` requires API key authentication and an `Idempotency-Key` header. The database enforces uniqueness on `(organization_id, idempotency_key)`.

A repeated request with the same payload returns the existing event as a duplicate. The same idempotency key with a different request hash is rejected.

This is idempotent storage through database constraints; it is not an exactly-once distributed processing claim.

## Quota And Rate Limit

Monthly quota is checked from `usage_events` for the current UTC billing period. FREE does not allow overage for gateway calls. PRO and BUSINESS allow overage.

Gateway requests also pass a Redis fixed-window rate limit per API key. Redis failure is fail-closed with `503 Service Unavailable`. Fixed-window boundary bursts are a known limitation.

## Invoice Generation

Invoice generation is organization-scoped and admin-only. A unique constraint on `(organization_id, billing_period)` makes generation idempotent.

Invoices are generated from REQUEST usage, the subscription plan, included quantity, base fee, and overage unit amount. Duplicate generation returns the existing invoice.

## Payment Webhook

`POST /api/webhooks/payments` is public but requires `X-Webhook-Signature`, an HMAC-SHA256 over the raw request body.

`providerEventId` is unique. Duplicate webhook delivery returns a duplicate response without creating another payment or ledger entry. Reusing the same provider event id with a different payload hash is rejected.

This is a mock payment provider integration, not a real PG integration.

## Ledger

Ledger entries are append-only. PostgreSQL triggers reject update and delete operations on `ledger_entries`.

Invoice issuance records receivable/revenue entries. Successful payment records cash/receivable entries. Entries are balanced by transaction group in tests.

This is a simplified ledger model and does not claim accounting compliance.

## Audit Log

Audit logs are append-only and organization-scoped. PostgreSQL triggers reject update and delete operations on `audit_logs`.

The implementation records organization creation, member changes, API key creation/revocation, subscription changes, invoice generation, webhook processing, and ledger group creation. Audit metadata stores key prefixes but not raw API keys or secrets.

## Observability

Micrometer counters are registered for usage ingestion, duplicate usage, quota exceeded, API key auth failures, invoice generation, webhook received/duplicates, ledger entries, and audit logs.

Metrics intentionally avoid high-cardinality tags such as organization id, user id, invoice id, email, and raw API key.

## Limitations

- No real AI provider calls.
- No real payment gateway integration.
- No refresh tokens, OAuth, SSO, or full IAM lifecycle.
- No background scheduler for invoice generation.
- No benchmark numbers are claimed.
