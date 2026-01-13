create table if not exists brand_distributor (
  id bigserial primary key,

  brand_id bigint not null references brand(id),
  distributor_id bigint not null references distributor(id),

  status varchar(20) not null default 'ACTIVE', -- ACTIVE / INACTIVE
  priority int not null default 0,

  offer_title varchar(150),
  offer_description text,
  offer_banner_url text,
  valid_till timestamptz,

  active boolean not null default true,
  deleted boolean not null default false,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint uq_brand_distributor unique (brand_id, distributor_id)
);

-- indexes for common queries
create index if not exists idx_brand_distributor_brand
  on brand_distributor (brand_id, active, deleted, priority);

create index if not exists idx_brand_distributor_distributor
  on brand_distributor (distributor_id, active, deleted);
