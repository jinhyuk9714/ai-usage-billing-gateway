create table invoices (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    billing_period varchar(7) not null,
    status varchar(30) not null,
    total_amount_minor bigint not null,
    currency varchar(3) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_invoices_org_period unique (organization_id, billing_period),
    constraint ck_invoices_status check (
        status in ('DRAFT', 'ISSUED', 'PAID', 'PAYMENT_FAILED', 'VOID')
    )
);

create table invoice_items (
    id uuid primary key,
    invoice_id uuid not null references invoices(id),
    description varchar(240) not null,
    quantity bigint not null,
    unit_amount_minor bigint not null,
    amount_minor bigint not null,
    created_at timestamptz not null,
    constraint ck_invoice_items_quantity_non_negative check (quantity >= 0),
    constraint ck_invoice_items_amount_non_negative check (amount_minor >= 0)
);
