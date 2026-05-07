package com.brandPitara.sfs.dashboard.common.enums;

public enum ReviewStatus {

    DRAFT,
    PENDING_REVIEW,
    RECHECK,
    APPROVED,
    REJECTED;

    public boolean isEditableByDataEntry() {
        return this == DRAFT || this == RECHECK || this == REJECTED;
    }

    public boolean isReviewable() {
        return this == PENDING_REVIEW || this == RECHECK;
    }

    public boolean isPubliclyVisible() {
        return this == APPROVED;
    }
}