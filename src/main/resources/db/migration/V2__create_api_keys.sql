create table api_keys (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    name varchar(120) not null,
    key_prefix varchar(32) not null,
    key_hash varchar(128) not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    last_used_at timestamptz,
    revoked_at timestamptz,
    constraint uk_api_keys_prefix unique (key_prefix),
    constraint uk_api_keys_hash unique (key_hash),
    constraint ck_api_keys_status check (status in ('ACTIVE', 'REVOKED'))
);
