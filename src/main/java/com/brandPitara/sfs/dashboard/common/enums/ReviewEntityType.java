package com.brandPitara.sfs.dashboard.common.enums;

public enum ReviewEntityType {

    PROJECT,
    PROJECT_MEDIA,
    PROJECT_FLOOR_PLAN,
    PROJECT_HIGHLIGHT,
    PROJECT_CONNECTIVITY,
    PROJECT_CONNECTIVITY_PLACE,
    PROJECT_MASTER_PLAN,
    PROJECT_METER,
    PROJECT_METER_SNAPSHOT,

    BUILDER,
    BUILDER_IMPROVEMENT_PROFILE,
    BUILDER_IMPROVEMENT_ACTION,
    BUILDER_IMPROVEMENT_ISSUE,
    BUILDER_AFTER_SALES_UPGRADE,
    BUILDER_IMPROVEMENT_TIMELINE,
    BUILDER_HIGHLIGHT_ITEM,

    COMPANY,
    PROMO_BANNER,
    APP_CONTENT,
    CITY,
    CATEGORY;

    public boolean isProjectRelated() {
        return this == PROJECT
                || this == PROJECT_MEDIA
                || this == PROJECT_FLOOR_PLAN
                || this == PROJECT_HIGHLIGHT
                || this == PROJECT_CONNECTIVITY
                || this == PROJECT_CONNECTIVITY_PLACE
                || this == PROJECT_MASTER_PLAN
                || this == PROJECT_METER
                || this == PROJECT_METER_SNAPSHOT;
    }

    public boolean isBuilderImprovementRelated() {
        return this == BUILDER_IMPROVEMENT_PROFILE
                || this == BUILDER_IMPROVEMENT_ACTION
                || this == BUILDER_IMPROVEMENT_ISSUE
                || this == BUILDER_AFTER_SALES_UPGRADE
                || this == BUILDER_IMPROVEMENT_TIMELINE;
    }
}
