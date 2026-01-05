create table if not exists deals (
    id bigserial primary key,
    deal_id varchar(100) not null,
    from_currency char(3) not null,
    to_currency char(3) not null,
    deal_ts timestamptz not null,
    amount numeric(38, 10) not null,
    created_at timestamptz not null default now()
);

-- Enforce idempotency: do not import the same deal twice.
create unique index if not exists uk_deals_deal_id on deals (deal_id);

create index if not exists ix_deals_deal_ts on deals (deal_ts);


