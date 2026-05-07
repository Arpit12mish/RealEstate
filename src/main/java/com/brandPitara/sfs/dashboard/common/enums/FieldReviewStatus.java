package com.brandPitara.sfs.dashboard.common.enums;

public enum FieldReviewStatus {

    RECHECK,
    WRONG,
    FIXED;

    public boolean isActiveIssue() {
        return this == RECHECK || this == WRONG;
    }

    public boolean isResolved() {
        return this == FIXED;
    }
}