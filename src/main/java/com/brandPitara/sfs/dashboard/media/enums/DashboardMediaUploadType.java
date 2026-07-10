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
    BUILDER_ANALYSIS_VIDEO_THUMBNAIL,

    // Brand dashboard uploads (Phase 2B.2) - brandId must already exist (see
    // DashboardMediaPresignServiceImpl#assertBrandExistsForBrandUpload), no temp/pre-create
    // upload path: this codebase has no existing temp-upload pattern for any entity, so brand
    // media follows the same create-entity-first convention as PROJECT_IMAGE/BUILDER_LOGO/etc.
    BRAND_LOGO,
    BRAND_HERO_IMAGE,
    BRAND_SKU_IMAGE,
    BRAND_CERTIFICATE_FILE,
    BRAND_PROMO_MEDIA,

    // Phase 2B.3 follow-up - the brand's own product category (Lamps/Mirrors/Kitchenware)
    // image, added after brand_product_category was introduced.
    BRAND_PRODUCT_CATEGORY_IMAGE,

    // Phase 4.8 - architect/interior designer company profile logo/cover image.
    COMPANY_LOGO,
    COMPANY_COVER_IMAGE,

    // Phase 4.8B - company hero/gallery/card images (company_media, usage_type
    // distinguishes HERO/GALLERY/CARD at the CRUD layer, not the upload layer).
    COMPANY_MEDIA_IMAGE,

    // Phase 4.8B - company certificate image (separate from BRAND_CERTIFICATE_FILE,
    // which allows PDF; this one is image-only, matching COMPANY_MEDIA_IMAGE).
    COMPANY_CERTIFICATE_IMAGE;

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

    // BRAND_CERTIFICATE_FILE is the only upload type that accepts either an image or a PDF.
    public boolean allowsImageOrPdf() {
        return this == BRAND_CERTIFICATE_FILE;
    }

    // BRAND_PROMO_MEDIA is the only upload type that accepts either an image or a video,
    // each with its own size limit - see DashboardMediaUploadValidator#validateFileSize.
    public boolean allowsImageOrVideo() {
        return this == BRAND_PROMO_MEDIA;
    }

    public boolean requiresImage() {
        return !requiresPdf() && !requiresVideo() && !requiresLottieJson()
                && !allowsImageOrPdf() && !allowsImageOrVideo();
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

    public boolean isBrandScoped() {
        return this == BRAND_LOGO
                || this == BRAND_HERO_IMAGE
                || this == BRAND_SKU_IMAGE
                || this == BRAND_CERTIFICATE_FILE
                || this == BRAND_PROMO_MEDIA
                || this == BRAND_PRODUCT_CATEGORY_IMAGE;
    }

    public boolean isCompanyScoped() {
        return this == COMPANY_LOGO
                || this == COMPANY_COVER_IMAGE
                || this == COMPANY_MEDIA_IMAGE
                || this == COMPANY_CERTIFICATE_IMAGE;
    }
}
