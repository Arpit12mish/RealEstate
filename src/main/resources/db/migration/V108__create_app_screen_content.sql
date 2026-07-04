create table if not exists app_screen_content (
    id bigserial primary key,
    screen_key varchar(50) not null,
    placement varchar(50) not null,
    media_type varchar(30) not null,
    media_url text not null,
    enabled boolean not null default true,
    background_color varchar(20) not null default '#000000',
    aspect_ratio numeric(8, 4),
    start_at timestamptz,
    end_at timestamptz,
    min_app_version varchar(40),
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_app_screen_content_screen_key
        check (screen_key in ('FAVORITES')),
    constraint chk_app_screen_content_placement
        check (placement in ('TOP_BANNER')),
    constraint chk_app_screen_content_media_type
        check (media_type in ('LOTTIE_JSON', 'VIDEO')),
    constraint chk_app_screen_content_media_url
        check (media_url ~* '^https?://.+'),
    constraint chk_app_screen_content_background_color
        check (background_color ~ '^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$'),
    constraint chk_app_screen_content_aspect_ratio
        check (aspect_ratio is null or (aspect_ratio > 0 and aspect_ratio <= 10)),
    constraint chk_app_screen_content_time_window
        check (end_at is null or start_at is null or end_at >= start_at)
);

create index if not exists idx_app_screen_content_lookup
    on app_screen_content(screen_key, placement, enabled, sort_order, id desc);

create index if not exists idx_app_screen_content_window
    on app_screen_content(start_at, end_at);

insert into app_screen_content (
    screen_key,
    placement,
    media_type,
    media_url,
    enabled,
    background_color,
    aspect_ratio,
    sort_order
)
select
    'FAVORITES',
    'TOP_BANNER',
    'LOTTIE_JSON',
    'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/fav/fav-animation-jitter.json',
    true,
    '#000000',
    4.02,
    0
where not exists (
    select 1
    from app_screen_content
    where screen_key = 'FAVORITES'
      and placement = 'TOP_BANNER'
);
