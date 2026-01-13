create table if not exists brand (
  id bigserial primary key,
  name varchar(150) not null,
  logo_url text,
  description text,
  active boolean not null default true,
  published boolean not null default false,
  priority int not null default 0,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

alter table brand add column if not exists deleted boolean not null default false;
