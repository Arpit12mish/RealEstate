package com.brandPitara.sfs.analytics.domain;

/**
 * Server-side allow-list of event names the ingestion pipeline accepts. Mobile/web
 * clients cannot introduce arbitrary event names - see the task's "controlled event
 * schema, not arbitrary frontend event names" requirement. An unrecognized name in a
 * batch is dropped (that event only, not the whole batch) rather than rejected with an
 * error, consistent with analytics being non-critical.
 *
 * Phase 1 scope only. CARD_IMPRESSION and SECTION_VIEWED are intentionally absent -
 * they depend on a virtualized-list migration for the search results screen and are
 * planned for Phase 2.
 */
public enum AnalyticsEventName {
    SEARCH_SUBMITTED,
    SEARCH_RESULT_CLICKED,
    PROJECT_VIEWED,
    BUILDER_VIEWED,
    ARTICLE_VIEWED,
    COMPANY_VIEWED,
    ARCHITECT_VIEWED,
    DESIGNER_VIEWED,
    CARD_CLICKED,
    SCROLL_DEPTH_REACHED,
    APP_SESSION_STARTED,
    APP_SESSION_ENDED
}
