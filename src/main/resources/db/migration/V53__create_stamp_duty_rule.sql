create table if not exists stamp_duty_rule (
    id bigserial primary key,

    state_name varchar(100) not null,
    city_name varchar(100) not null,

    buyer_type varchar(30) not null,
    property_category varchar(30) not null,

    stamp_duty_percent numeric(8,4) not null,
    registration_percent numeric(8,4) not null,
    local_body_tax_percent numeric(8,4) not null,

    effective_from date not null,
    effective_to date null,

    active boolean not null default true,

    source_note varchar(500),

    constraint chk_stamp_duty_percent_non_negative
        check (stamp_duty_percent >= 0),

    constraint chk_registration_percent_non_negative
        check (registration_percent >= 0),

    constraint chk_local_body_tax_percent_non_negative
        check (local_body_tax_percent >= 0),

    constraint chk_stamp_duty_percent_max
        check (stamp_duty_percent <= 100),

    constraint chk_registration_percent_max
        check (registration_percent <= 100),

    constraint chk_local_body_tax_percent_max
        check (local_body_tax_percent <= 100),

    constraint chk_stamp_duty_rule_date_range
        check (effective_to is null or effective_to >= effective_from)
);

create index if not exists idx_sd_city
    on stamp_duty_rule (city_name);

create index if not exists idx_sd_lookup
    on stamp_duty_rule (state_name, city_name, buyer_type, property_category, active);

create index if not exists idx_sd_effective
    on stamp_duty_rule (effective_from, effective_to);