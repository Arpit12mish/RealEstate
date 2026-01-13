create table if not exists content_version (
  key varchar(50) primary key,
  version bigint not null
);

insert into content_version(key, version) values
('HOME', 1),
('BRANDS', 1),
('BUILDERS', 1)
on conflict (key) do nothing;
