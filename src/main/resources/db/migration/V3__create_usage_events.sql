create table usage_events (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    api_key_id uuid not null references api_keys(id),
    idempotency_key varchar(160) not null,
    request_hash varchar(128) not null,
    metric varchar(40) not null,
    quantity bigint not null,
    occurred_at timestamptz not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null,
    constraint uk_usage_org_idempotency unique (organization_id, idempotency_key),
    constraint ck_usage_quantity_positive check (quantity > 0),
    constraint ck_usage_metric check (metric in ('REQUEST'))
);
