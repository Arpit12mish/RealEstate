package com.brandPitara.sfs.dashboard.common.enums;

public enum DashboardRole {

    ADMIN,
    REVIEWER,
    DATA_ENTRY,

    /**
     * Least-privilege dashboard identity for CMS staff. Actual CMS capability
     * is assigned with DashboardPermission rather than additional role names.
     */
    CONTENT_STAFF;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean canEnterData() {
        return this == ADMIN || this == DATA_ENTRY;
    }

    public boolean canReviewData() {
        return this == ADMIN || this == REVIEWER;
    }

    public boolean canManageDashboardUsers() {
        return this == ADMIN;
    }
}
