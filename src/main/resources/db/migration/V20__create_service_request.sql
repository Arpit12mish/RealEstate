-- service_request table
create table if not exists service_request (
  id bigserial primary key,
  customer_user_id bigint not null,
  status varchar(20) not null,
  city_id bigint not null,
  locality varchar(120),
  pincode varchar(12) not null,
  notes text,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint fk_sr_customer foreign key (customer_user_id) references users(id),
  constraint fk_sr_city foreign key (city_id) references city(id)
);

create index if not exists idx_sr_status_city on service_request(status, city_id);
create index if not exists idx_sr_pincode on service_request(pincode);
create index if not exists idx_sr_customer on service_request(customer_user_id);

-- join table for multiple categories on request
create table if not exists service_request_category (
  service_request_id bigint not null,
  category_id bigint not null,
  primary key (service_request_id, category_id),
  constraint fk_src_req foreign key (service_request_id) references service_request(id) on delete cascade,
  constraint fk_src_cat foreign key (category_id) references category(id)
);

create index if not exists idx_src_category_id on service_request_category(category_id);
