package com.brandPitara.sfs.dashboard.common.enums;

public enum DashboardRole {

    ADMIN,
    REVIEWER,
    DATA_ENTRY;

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