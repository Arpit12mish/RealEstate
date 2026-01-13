create table if not exists distributor (
  id bigserial primary key,
  name varchar(200) not null,

  phone varchar(30),
  whatsapp varchar(30),
  email varchar(200),

  address_line1 varchar(255),
  address_line2 varchar(255),
  pincode varchar(12),

  city_id bigint,

  latitude double precision,
  longitude double precision,

  active boolean not null default true,
  deleted boolean not null default false,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- indexes for scale
create index if not exists idx_distributor_city_active
  on distributor (city_id, active, deleted);

create index if not exists idx_distributor_phone
  on distributor (phone);

create index if not exists idx_distributor_whatsapp
  on distributor (whatsapp);
