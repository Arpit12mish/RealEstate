create table if not exists interior_cost_addon_rule (
    id bigserial primary key,

    company_id bigint not null,
    city_name varchar(100) not null,

    package_type varchar(40) not null,
    addon_type varchar(40) not null,

    unit_price numeric(18,2) not null,

    active boolean not null default true,

    effective_from date not null,
    effective_to date null,

    source_note varchar(500),

    constraint fk_interior_cost_addon_rule_company
        foreign key (company_id) references company(id),

    constraint chk_interior_cost_addon_rule_unit_price_non_negative
        check (unit_price >= 0),

    constraint chk_interior_cost_addon_rule_date_range
        check (effective_to is null or effective_to >= effective_from)
);

create index if not exists idx_icar_city
    on interior_cost_addon_rule (city_name);

create index if not exists idx_icar_lookup
    on interior_cost_addon_rule (city_name, package_type, addon_type, active);

create index if not exists idx_icar_company
    on interior_cost_addon_rule (company_id, active);

create index if not exists idx_icar_effective
    on interior_cost_addon_rule (effective_from, effective_to);