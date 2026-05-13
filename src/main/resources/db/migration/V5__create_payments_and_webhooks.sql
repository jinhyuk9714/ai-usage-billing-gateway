create table payments (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    invoice_id uuid not null references invoices(id),
    provider_event_id varchar(160) not null,
    status varchar(30) not null,
    amount_minor bigint not null,
    currency varchar(3) not null,
    created_at timestamptz not null,
    constraint uk_payments_provider_event unique (provider_event_id),
    constraint ck_payments_status check (status in ('SUCCEEDED', 'FAILED', 'REFUNDED'))
);

create table payment_webhook_events (
    id uuid primary key,
    provider_event_id varchar(160) not null,
    event_type varchar(80) not null,
    payload_hash varchar(128) not null,
    processed_at timestamptz not null,
    duplicate boolean not null,
    constraint uk_webhook_provider_event unique (provider_event_id)
);
