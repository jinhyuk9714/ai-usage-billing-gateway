create table users (
    id uuid primary key,
    email varchar(320) not null,
    password_hash varchar(255) not null,
    created_at timestamptz not null,
    constraint uk_users_email unique (email)
);

create table organizations (
    id uuid primary key,
    name varchar(200) not null,
    created_at timestamptz not null
);

create table organization_members (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    user_id uuid not null references users(id),
    role varchar(20) not null,
    created_at timestamptz not null,
    constraint uk_org_members_org_user unique (organization_id, user_id),
    constraint ck_org_members_role check (role in ('OWNER', 'ADMIN', 'MEMBER'))
);

create table plans (
    id uuid primary key,
    code varchar(40) not null,
    name varchar(120) not null,
    included_quantity bigint not null,
    overage_unit_amount_minor bigint not null,
    base_amount_minor bigint not null,
    currency varchar(3) not null,
    overage_allowed boolean not null,
    created_at timestamptz not null,
    constraint uk_plans_code unique (code)
);

create table subscriptions (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    plan_id uuid not null references plans(id),
    status varchar(20) not null,
    started_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_subscriptions_organization unique (organization_id),
    constraint ck_subscriptions_status check (status in ('ACTIVE', 'CANCELED'))
);

insert into plans (
    id,
    code,
    name,
    included_quantity,
    overage_unit_amount_minor,
    base_amount_minor,
    currency,
    overage_allowed,
    created_at
) values
    ('00000000-0000-0000-0000-000000000101', 'FREE', 'Free', 10000, 0, 0, 'USD', false, now()),
    ('00000000-0000-0000-0000-000000000102', 'PRO', 'Pro', 100000, 2, 2900, 'USD', true, now()),
    ('00000000-0000-0000-0000-000000000103', 'BUSINESS', 'Business', 1000000, 1, 19900, 'USD', true, now());
