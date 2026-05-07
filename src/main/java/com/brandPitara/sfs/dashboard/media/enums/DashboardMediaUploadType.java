package com.brandPitara.sfs.dashboard.media.enums;

public enum DashboardMediaUploadType {

    PROJECT_IMAGE,
    BUILDER_LOGO,
    FLOOR_PLAN_IMAGE,
    CONNECTIVITY_MAP,
    BROCHURE_PDF;

    public boolean requiresPdf() {
        return this == BROCHURE_PDF;
    }

    public boolean requiresImage() {
        return !requiresPdf();
    }

    public boolean isProjectScoped() {
        return this == PROJECT_IMAGE
                || this == FLOOR_PLAN_IMAGE
                || this == CONNECTIVITY_MAP
                || this == BROCHURE_PDF;
    }

    public boolean isBuilderScoped() {
        return this == BUILDER_LOGO;
    }
}
