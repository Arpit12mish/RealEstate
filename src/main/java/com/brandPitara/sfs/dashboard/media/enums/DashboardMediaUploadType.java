package com.brandPitara.sfs.dashboard.media.enums;

public enum DashboardMediaUploadType {

    PROJECT_IMAGE,
    BUILDER_LOGO,
    FLOOR_PLAN_IMAGE,
    MASTER_PLAN_IMAGE,
    CONNECTIVITY_MAP,
    BROCHURE_PDF,
    CITY_COVER_IMAGE,
    INSTAGRAM_REEL_THUMBNAIL,
    INSTAGRAM_REEL_PREVIEW_VIDEO,
    HOME_LOTTIE_JSON,
    APP_SCREEN_LOTTIE_JSON,
    APP_SCREEN_VIDEO,
    BUILDER_HIGHLIGHT_IMAGE,
    BUILDER_HIGHLIGHT_THUMBNAIL,
    BUILDER_ANALYSIS_VIDEO_THUMBNAIL;

    public boolean requiresPdf() {
        return this == BROCHURE_PDF;
    }

    public boolean requiresVideo() {
        return this == INSTAGRAM_REEL_PREVIEW_VIDEO
                || this == APP_SCREEN_VIDEO;
    }

    public boolean requiresLottieJson() {
        return this == HOME_LOTTIE_JSON
                || this == APP_SCREEN_LOTTIE_JSON;
    }

    public boolean requiresImage() {
        return !requiresPdf() && !requiresVideo() && !requiresLottieJson();
    }

    public boolean isProjectScoped() {
        return this == PROJECT_IMAGE
                || this == FLOOR_PLAN_IMAGE
                || this == MASTER_PLAN_IMAGE
                || this == CONNECTIVITY_MAP
                || this == BROCHURE_PDF;
    }

    public boolean isBuilderScoped() {
        return this == BUILDER_LOGO
                || this == BUILDER_HIGHLIGHT_IMAGE
                || this == BUILDER_HIGHLIGHT_THUMBNAIL
                || this == BUILDER_ANALYSIS_VIDEO_THUMBNAIL;
    }

    public boolean isCityScoped() {
        return this == CITY_COVER_IMAGE;
    }
}
