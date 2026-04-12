create table if not exists user_favorite (
    id bigserial primary key,
    user_id bigint not null,
    target_type varchar(30) not null,
    target_id bigint not null,
    created_at timestamptz not null default now(),

    constraint fk_user_favorite_user
        foreign key (user_id)
        references users(id)
        on delete cascade,

    constraint uk_user_favorite_user_target
        unique (user_id, target_type, target_id)
);

create index if not exists idx_user_favorite_user_id
    on user_favorite(user_id);

create index if not exists idx_user_favorite_target
    on user_favorite(target_type, target_id);