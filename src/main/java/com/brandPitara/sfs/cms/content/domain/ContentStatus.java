package com.brandPitara.sfs.cms.content.domain;

/**
 * The full editorial lifecycle. All transitions between these statuses are implemented and
 * exposed via {@code ContentWorkflowController} — see {@code ContentWorkflowTransitionPolicy}
 * for the exact allowed-transition table.
 */
public enum ContentStatus {
    DRAFT,
    IN_REVIEW,
    CHANGES_REQUESTED,
    APPROVED,
    PUBLISHED,
    UNPUBLISHED,
    ARCHIVED
}
