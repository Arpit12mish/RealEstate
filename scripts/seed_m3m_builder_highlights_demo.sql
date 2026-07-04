-- =========================================================
-- Local/dev-only seed: M3M Builder Highlight demo content
-- =========================================================
-- Purpose:
--   Adds temporary published Builder Highlight data for mobile/dashboard testing.
--
-- Important:
--   - Do NOT add this as a production Flyway migration.
--   - Demo content is intentionally labeled Sample/Demo.
--   - Remove or replace before production launch.
--
-- Usage:
--   PGPASSWORD='bp@sfs2025' psql -h localhost -p 5432 -U sfs_user -d sfs_db \
--     -f scripts/seed_m3m_builder_highlights_demo.sql
-- =========================================================

begin;

-- Verified locally before this script was created:
--   builder.id = 2
--   builder.name = 'M3M'
--   active = true, published = true, deleted = false

delete from builder_highlight_item
where builder_id = 2
  and title in (
      'Sample Update: Possession Timeline Review',
      'Sample CSR: Education Support Drive',
      'Sample News: M3M Expansion Coverage',
      'Sample SFS Analysis: M3M Builder Overview'
  );

with item as (
    insert into builder_highlight_item (
        builder_id,
        highlight_type,
        source_type,
        media_type,
        title,
        subtitle,
        summary,
        body,
        tag_label,
        tag_type,
        featured,
        verified,
        public_visible,
        active,
        sort_order,
        status,
        published_at,
        created_by,
        updated_by,
        approved_by,
        approved_at
    )
    values (
        2,
        'BUILDER_UPDATE',
        'BUILDER_OFFICIAL',
        'NONE',
        'Sample Update: Possession Timeline Review',
        'Builder communication update',
        'Demo update for testing: M3M has shared a sample possession timeline communication for selected project phases. This content is added only for app testing.',
        'This is dummy Builder Highlight content used to validate the Builder Updates section on mobile. Replace this with verified builder communication before production use.',
        'Demo Update',
        'INFO',
        true,
        false,
        true,
        true,
        1,
        'PUBLISHED',
        now(),
        1,
        1,
        1,
        now()
    )
    returning id
)
insert into builder_highlight_point (
    highlight_item_id,
    point_type,
    title,
    text,
    icon_key,
    display_order,
    active
)
select
    id,
    'KEY_TAKEAWAY',
    'Timeline communication',
    'Sample update added to test how builder-side updates appear in the app.',
    'calendar-check',
    1,
    true
from item;

with item as (
    insert into builder_highlight_item (
        builder_id,
        highlight_type,
        source_type,
        media_type,
        title,
        subtitle,
        summary,
        body,
        tag_label,
        tag_type,
        thumbnail_url,
        image_url,
        featured,
        verified,
        public_visible,
        active,
        sort_order,
        status,
        published_at,
        created_by,
        updated_by,
        approved_by,
        approved_at
    )
    values (
        2,
        'SOCIAL_IMPACT',
        'BUILDER_OFFICIAL',
        'IMAGE',
        'Sample CSR: Education Support Drive',
        'Community support initiative',
        'Demo social impact content for testing the Social Impact section. Represents education support, digital learning, and local community upliftment.',
        'This is dummy content for mobile UI testing. Replace with verified CSR data, actual images, and official source references before public launch.',
        'Community',
        'CSR',
        'https://images.unsplash.com/photo-1497486751825-1233686d5d80?w=1200',
        'https://images.unsplash.com/photo-1497486751825-1233686d5d80?w=1200',
        true,
        false,
        true,
        true,
        2,
        'PUBLISHED',
        now(),
        1,
        1,
        1,
        now()
    )
    returning id
)
insert into builder_highlight_point (
    highlight_item_id,
    point_type,
    title,
    text,
    icon_key,
    display_order,
    active
)
select id, 'IMPACT_METRIC', '3,200+ beneficiaries / year',
       'Sample impact metric used only for dashboard and mobile testing.',
       'users', 1, true
from item
union all
select id, 'KEY_TAKEAWAY', 'Education support',
       'Demo content showing how community support initiatives will appear.',
       'book-open', 2, true
from item;

with item as (
    insert into builder_highlight_item (
        builder_id,
        highlight_type,
        source_type,
        media_type,
        title,
        subtitle,
        summary,
        body,
        tag_label,
        tag_type,
        thumbnail_url,
        external_url,
        webview_enabled,
        publisher_name,
        author_label,
        read_time_minutes,
        published_at,
        featured,
        verified,
        public_visible,
        active,
        sort_order,
        status,
        created_by,
        updated_by,
        approved_by,
        approved_at
    )
    values (
        2,
        'NEWS_ARTICLE',
        'EXTERNAL_NEWS',
        'WEBVIEW',
        'Sample News: M3M Expansion Coverage',
        'External article preview',
        'Demo news article entry for testing WebView behavior in the mobile app. Replace with an actual verified article URL before production.',
        'This sample validates News & Articles rendering and external WebView navigation.',
        'News',
        'ARTICLE',
        'https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=1200',
        'https://www.google.com/search?q=M3M+real+estate+news',
        true,
        'Demo Publisher',
        'SFS Demo Desk',
        4,
        '2026-07-01T10:00:00Z',
        false,
        false,
        true,
        true,
        3,
        'PUBLISHED',
        1,
        1,
        1,
        now()
    )
    returning id
)
insert into builder_highlight_point (
    highlight_item_id,
    point_type,
    title,
    text,
    icon_key,
    display_order,
    active
)
select
    id,
    'SUMMARY',
    'External article test',
    'Used to verify that external article cards and WebView navigation work correctly.',
    'newspaper',
    1,
    true
from item;

with item as (
    insert into builder_highlight_item (
        builder_id,
        highlight_type,
        source_type,
        media_type,
        title,
        subtitle,
        summary,
        body,
        tag_label,
        tag_type,
        youtube_video_id,
        video_url,
        thumbnail_url,
        featured,
        verified,
        public_visible,
        active,
        sort_order,
        status,
        published_at,
        created_by,
        updated_by,
        approved_by,
        approved_at
    )
    values (
        2,
        'SFS_ANALYSIS',
        'SFS_EDITORIAL',
        'YOUTUBE',
        'Sample SFS Analysis: M3M Builder Overview',
        'Builder performance and investment analysis',
        'Demo SFS analysis content for testing YouTube playback, video card rendering, advantages, and considerations in the mobile app.',
        'This sample SFS analysis reviews builder positioning, buyer trust signals, project visibility, and investment considerations. This is dummy testing content and must be replaced before production use.',
        'SFS Analysis',
        'VIDEO',
        'jNQXAC9IVRw',
        'https://www.youtube.com/watch?v=jNQXAC9IVRw',
        '',
        true,
        true,
        true,
        true,
        4,
        'PUBLISHED',
        now(),
        1,
        1,
        1,
        now()
    )
    returning id
)
insert into builder_highlight_point (
    highlight_item_id,
    point_type,
    title,
    text,
    icon_key,
    display_order,
    active
)
select id, 'ADVANTAGE', 'Strong brand recall',
       'M3M has strong brand visibility in key NCR real estate corridors.',
       'trend-up', 1, true
from item
union all
select id, 'ADVANTAGE', 'Premium project positioning',
       'Several projects are positioned around premium residential and commercial demand zones.',
       'building', 2, true
from item
union all
select id, 'DISADVANTAGE', 'Timeline monitoring required',
       'Buyers should review project-wise timelines, approvals, and delivery history before decision-making.',
       'alert-triangle', 3, true
from item
union all
select id, 'DISADVANTAGE', 'Verify project-specific details',
       'Pricing, possession, RERA, and amenities should be checked at the individual project level.',
       'shield-alert', 4, true
from item;

commit;

select
    id,
    highlight_type,
    title,
    status,
    public_visible,
    active,
    deleted_at
from builder_highlight_item
where builder_id = 2
  and title in (
      'Sample Update: Possession Timeline Review',
      'Sample CSR: Education Support Drive',
      'Sample News: M3M Expansion Coverage',
      'Sample SFS Analysis: M3M Builder Overview'
  )
order by sort_order, id;
