create table audit_logs (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    actor_user_id uuid references users(id),
    action varchar(80) not null,
    target_type varchar(80) not null,
    target_id uuid,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null
);

create or replace function prevent_audit_log_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'audit_logs are append-only';
end;
$$;

create trigger trg_prevent_audit_update
before update on audit_logs
for each row execute function prevent_audit_log_mutation();

create trigger trg_prevent_audit_delete
before delete on audit_logs
for each row execute function prevent_audit_log_mutation();
