-- =========================================================
-- V109__create_builder_highlight_tables.sql
-- Builder Highlight replacement module for Builder Improvement
-- =========================================================

create table if not exists builder_highlight_item (
    id bigserial primary key,

    builder_id bigint not null,
    project_id bigint,
    city_id bigint,

    highlight_type varchar(40) not null,
    source_type varchar(40) not null,
    media_type varchar(40) not null,

    title varchar(180) not null,
    subtitle varchar(255),
    summary text,
    body text,
    tag_label varchar(80),
    tag_type varchar(60),

    thumbnail_url text,
    image_url text,
    video_url text,
    youtube_video_id varchar(80),
    external_url text,
    webview_enabled boolean not null default false,

    publisher_name varchar(180),
    author_label varchar(150),
    read_time_minutes integer not null default 0,
    published_at timestamptz,

    featured boolean not null default false,
    verified boolean not null default false,
    public_visible boolean not null default false,
    active boolean not null default true,
    sort_order integer not null default 0,

    status varchar(40) not null default 'DRAFT',

    created_by bigint,
    updated_by bigint,
    approved_by bigint,
    approved_at timestamptz,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,

    constraint fk_builder_highlight_item_builder
        foreign key (builder_id) references builder(id),
    constraint fk_builder_highlight_item_project
        foreign key (project_id) references project(id),
    constraint fk_builder_highlight_item_city
        foreign key (city_id) references city(id),
    constraint fk_builder_highlight_item_created_by
        foreign key (created_by) references dashboard_users(id) on delete set null,
    constraint fk_builder_highlight_item_updated_by
        foreign key (updated_by) references dashboard_users(id) on delete set null,
    constraint fk_builder_highlight_item_approved_by
        foreign key (approved_by) references dashboard_users(id) on delete set null,

    constraint chk_builder_highlight_type
        check (highlight_type in (
            'BUILDER_UPDATE',
            'SOCIAL_IMPACT',
            'NEWS_ARTICLE',
            'SFS_ANALYSIS'
        )),
    constraint chk_builder_highlight_source_type
        check (source_type in (
            'BUILDER_OFFICIAL',
            'SFS_EDITORIAL',
            'EXTERNAL_NEWS',
            'SOCIAL_MEDIA',
            'OTHER'
        )),
    constraint chk_builder_highlight_media_type
        check (media_type in (
            'IMAGE',
            'VIDEO',
            'YOUTUBE',
            'WEBVIEW',
            'NONE'
        )),
    constraint chk_builder_highlight_status
        check (status in (
            'DRAFT',
            'PENDING_REVIEW',
            'PUBLISHED',
            'ARCHIVED'
        )),
    constraint chk_builder_highlight_read_time
        check (read_time_minutes >= 0),
    constraint chk_builder_highlight_sort_order
        check (sort_order >= 0),
    constraint chk_builder_highlight_webview_url
        check (media_type <> 'WEBVIEW' or external_url is not null),
    constraint chk_builder_highlight_youtube_ref
        check (media_type <> 'YOUTUBE' or youtube_video_id is not null or video_url is not null),
    constraint chk_builder_highlight_external_news_publisher
        check (highlight_type <> 'NEWS_ARTICLE' or source_type <> 'EXTERNAL_NEWS' or publisher_name is not null)
);

create table if not exists builder_highlight_point (
    id bigserial primary key,

    highlight_item_id bigint not null,
    point_type varchar(40) not null,
    title varchar(180),
    text text,
    icon_key varchar(80),
    display_order integer not null default 0,
    active boolean not null default true,

    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint fk_builder_highlight_point_item
        foreign key (highlight_item_id) references builder_highlight_item(id) on delete cascade,
    constraint chk_builder_highlight_point_type
        check (point_type in (
            'ADVANTAGE',
            'DISADVANTAGE',
            'SUMMARY',
            'IMPACT_METRIC',
            'KEY_TAKEAWAY'
        )),
    constraint chk_builder_highlight_point_display_order
        check (display_order >= 0)
);

create index if not exists idx_builder_highlight_item_builder
    on builder_highlight_item(builder_id);

create index if not exists idx_builder_highlight_item_type
    on builder_highlight_item(highlight_type);

create index if not exists idx_builder_highlight_item_status
    on builder_highlight_item(status);

create index if not exists idx_builder_highlight_item_public_visible
    on builder_highlight_item(public_visible);

create index if not exists idx_builder_highlight_item_active
    on builder_highlight_item(active);

create index if not exists idx_builder_highlight_item_featured
    on builder_highlight_item(featured);

create index if not exists idx_builder_highlight_item_published_at
    on builder_highlight_item(published_at);

create index if not exists idx_builder_highlight_item_sort_order
    on builder_highlight_item(sort_order);

create index if not exists idx_builder_highlight_item_deleted_at
    on builder_highlight_item(deleted_at);

create index if not exists idx_builder_highlight_item_public_listing
    on builder_highlight_item(builder_id, status, public_visible, active, deleted_at, highlight_type);

create index if not exists idx_builder_highlight_item_public_sort
    on builder_highlight_item(builder_id, highlight_type, status, public_visible, active, deleted_at, featured desc, sort_order asc, published_at desc, id desc);

create index if not exists idx_builder_highlight_point_item_sort
    on builder_highlight_point(highlight_item_id, active, display_order, id);

insert into content_version(key, version)
values ('BUILDER_HIGHLIGHT', 1)
on conflict (key) do nothing;
