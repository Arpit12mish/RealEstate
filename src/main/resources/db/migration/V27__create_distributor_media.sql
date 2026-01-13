create table if not exists distributor_media (
  id bigserial primary key,

  distributor_id bigint not null references distributor(id),
  media_type varchar(20) not null, -- IMAGE / VIDEO
  url text not null,

  caption varchar(255),
  sort_order int not null default 0,

  active boolean not null default true,
  deleted boolean not null default false,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_distributor_media_distributor
  on distributor_media (distributor_id, deleted, active, sort_order);
