create table if not exists service_request_interest (
  id bigserial primary key,
  service_request_id bigint not null,
  provider_id bigint not null,
  status varchar(20) not null,
  message text,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint fk_sri_request foreign key (service_request_id) references service_request(id) on delete cascade,
  constraint fk_sri_provider foreign key (provider_id) references provider_profile(id) on delete cascade,
  constraint uk_sri_req_provider unique (service_request_id, provider_id)
);

create index if not exists idx_sri_request on service_request_interest(service_request_id);
create index if not exists idx_sri_provider on service_request_interest(provider_id);
create index if not exists idx_sri_status on service_request_interest(status);
