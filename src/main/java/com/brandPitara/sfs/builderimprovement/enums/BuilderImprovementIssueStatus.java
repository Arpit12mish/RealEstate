package com.brandPitara.sfs.builderimprovement.enums;

public enum BuilderImprovementIssueStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    REOPENED,
    CANCELLED;

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean isOpen() {
        return this == PENDING || this == IN_PROGRESS || this == REOPENED;
    }
}