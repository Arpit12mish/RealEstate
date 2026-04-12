create table if not exists interior_cost_rule (
    id bigserial primary key,

    company_id bigint not null,
    city_name varchar(100) not null,

    property_type varchar(40) not null,
    area_unit varchar(20) not null,
    bhk_type varchar(40) not null,
    package_type varchar(40) not null,
    scope_type varchar(40) not null,

    min_area numeric(18,2) not null,
    max_area numeric(18,2) not null,

    base_rate_per_unit numeric(18,2) not null,
    minimum_project_cost numeric(18,2) not null,

    contingency_percent numeric(8,4) not null,
    tax_percent numeric(8,4) not null,

    active boolean not null default true,

    effective_from date not null,
    effective_to date null,

    source_note varchar(500),

    constraint fk_interior_cost_rule_company
        foreign key (company_id) references company(id),

    constraint chk_interior_cost_rule_min_area_positive
        check (min_area > 0),

    constraint chk_interior_cost_rule_max_area_positive
        check (max_area > 0),

    constraint chk_interior_cost_rule_area_range
        check (max_area >= min_area),

    constraint chk_interior_cost_rule_base_rate_positive
        check (base_rate_per_unit > 0),

    constraint chk_interior_cost_rule_min_project_non_negative
        check (minimum_project_cost >= 0),

    constraint chk_interior_cost_rule_contingency_non_negative
        check (contingency_percent >= 0),

    constraint chk_interior_cost_rule_tax_non_negative
        check (tax_percent >= 0),

    constraint chk_interior_cost_rule_contingency_max
        check (contingency_percent <= 100),

    constraint chk_interior_cost_rule_tax_max
        check (tax_percent <= 100),

    constraint chk_interior_cost_rule_date_range
        check (effective_to is null or effective_to >= effective_from)
);

create index if not exists idx_icr_city
    on interior_cost_rule (city_name);

create index if not exists idx_icr_lookup
    on interior_cost_rule (city_name, property_type, package_type, scope_type, bhk_type, active);

create index if not exists idx_icr_company
    on interior_cost_rule (company_id, active);

create index if not exists idx_icr_effective
    on interior_cost_rule (effective_from, effective_to);