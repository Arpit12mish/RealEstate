package com.brandPitara.sfs.dashboard.common.enums;

public enum ReviewEntityType {

    PROJECT,
    PROJECT_MEDIA,
    PROJECT_FLOOR_PLAN,
    PROJECT_HIGHLIGHT,
    PROJECT_CONNECTIVITY,
    PROJECT_CONNECTIVITY_PLACE,
    PROJECT_METER,
    PROJECT_METER_SNAPSHOT,
    BUILDER,
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
                || this == PROJECT_METER
                || this == PROJECT_METER_SNAPSHOT;
    }
}