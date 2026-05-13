create table ledger_entries (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    invoice_id uuid references invoices(id),
    payment_id uuid references payments(id),
    transaction_group_id varchar(180) not null,
    type varchar(60) not null,
    account varchar(80) not null,
    direction varchar(20) not null,
    amount_minor bigint not null,
    currency varchar(3) not null,
    idempotency_key varchar(200) not null,
    created_at timestamptz not null,
    constraint uk_ledger_idempotency unique (idempotency_key),
    constraint ck_ledger_direction check (direction in ('DEBIT', 'CREDIT')),
    constraint ck_ledger_amount_non_negative check (amount_minor >= 0)
);

create or replace function prevent_ledger_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'ledger_entries are append-only';
end;
$$;

create trigger trg_prevent_ledger_update
before update on ledger_entries
for each row execute function prevent_ledger_mutation();

create trigger trg_prevent_ledger_delete
before delete on ledger_entries
for each row execute function prevent_ledger_mutation();
