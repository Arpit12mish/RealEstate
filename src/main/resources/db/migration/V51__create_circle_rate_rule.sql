create table if not exists circle_rate_rule (
    id bigserial primary key,

    state_name varchar(100) not null,
    city_name varchar(100) not null,
    locality_name varchar(150) not null,

    property_type varchar(40) not null,
    unit_type varchar(20) not null,
    formula_type varchar(40) not null,

    rate_per_unit numeric(18,2) not null,

    effective_from date not null,
    effective_to date null,

    active boolean not null default true,

    source_note varchar(500),

    constraint chk_circle_rate_rule_rate_positive
        check (rate_per_unit > 0),

    constraint chk_circle_rate_rule_date_range
        check (effective_to is null or effective_to >= effective_from)
);

create index if not exists idx_cr_city
    on circle_rate_rule (city_name);

create index if not exists idx_cr_city_locality
    on circle_rate_rule (city_name, locality_name);

create index if not exists idx_cr_lookup
    on circle_rate_rule (state_name, city_name, locality_name, property_type, active);

create index if not exists idx_cr_effective
    on circle_rate_rule (effective_from, effective_to);