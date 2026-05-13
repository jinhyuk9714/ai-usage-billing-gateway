create index idx_org_members_user on organization_members(user_id);
create index idx_api_keys_org on api_keys(organization_id);
create index idx_usage_org_occurred on usage_events(organization_id, occurred_at);
create index idx_invoices_org_status on invoices(organization_id, status);
create index idx_payments_invoice on payments(invoice_id);
create index idx_ledger_group on ledger_entries(transaction_group_id);
create index idx_audit_org_created on audit_logs(organization_id, created_at desc);
