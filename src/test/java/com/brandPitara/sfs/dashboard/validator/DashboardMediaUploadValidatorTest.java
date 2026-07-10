package com.brandPitara.sfs.dashboard.validator;

import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanUpsertRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardMediaUploadValidatorTest {

    private final DashboardMediaUploadValidator validator = new DashboardMediaUploadValidator();

    @Test
    void cityCoverImageRequiresCityId() {
        assertThatThrownBy(() -> validator.validateContextIds(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                null,
                null,
                null,
                null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CITY_COVER_IMAGE requires cityId");
    }

    @Test
    void cityCoverImageRejectsPdfContentType() {
        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                "application/pdf"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an image content type");
    }

    @Test
    void cityCoverImageRejectsUnsupportedImageContentType() {
        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                "image/gif"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only image/jpeg, image/jpg, image/png, image/webp");
    }

    @Test
    void masterPlanImageRequiresProjectId() {
        assertThatThrownBy(() -> validator.validateContextIds(
                DashboardMediaUploadType.MASTER_PLAN_IMAGE,
                null,
                null,
                null,
                null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MASTER_PLAN_IMAGE requires projectId");
    }

    @Test
    void masterPlanImageAcceptsSupportedImageTypesAndRejectsPdf() {
        validator.validateContentType(DashboardMediaUploadType.MASTER_PLAN_IMAGE, "image/jpeg");
        validator.validateContentType(DashboardMediaUploadType.MASTER_PLAN_IMAGE, "image/jpg");
        validator.validateContentType(DashboardMediaUploadType.MASTER_PLAN_IMAGE, "image/png");
        validator.validateContentType(DashboardMediaUploadType.MASTER_PLAN_IMAGE, "image/webp");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.MASTER_PLAN_IMAGE,
                "application/pdf"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an image content type");
    }

    @Test
    void cityCoverImageRejectsOversizedFile() {
        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                "image/jpeg",
                2L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 2 MB");
    }

    @Test
    void instagramReelThumbnailAcceptsSupportedImages() {
        validator.validateContentType(DashboardMediaUploadType.INSTAGRAM_REEL_THUMBNAIL, "image/jpeg");
        validator.validateContentType(DashboardMediaUploadType.INSTAGRAM_REEL_THUMBNAIL, "image/jpg");
        validator.validateContentType(DashboardMediaUploadType.INSTAGRAM_REEL_THUMBNAIL, "image/png");
        validator.validateContentType(DashboardMediaUploadType.INSTAGRAM_REEL_THUMBNAIL, "image/webp");
    }

    @Test
    void instagramReelPreviewVideoAcceptsMp4AndRejectsOversize() {
        validator.validateContentType(DashboardMediaUploadType.INSTAGRAM_REEL_PREVIEW_VIDEO, "video/mp4");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.INSTAGRAM_REEL_PREVIEW_VIDEO,
                "video/quicktime"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only video/mp4");

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.INSTAGRAM_REEL_PREVIEW_VIDEO,
                "video/mp4",
                5L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 5 MB");
    }

    @Test
    void homeLottieJsonAcceptsApplicationJson() {
        validator.validateContentType(DashboardMediaUploadType.HOME_LOTTIE_JSON, "application/json");
    }

    @Test
    void homeLottieJsonRejectsImage() {
        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.HOME_LOTTIE_JSON,
                "image/png"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only application/json");
    }

    @Test
    void homeLottieJsonRejectsPdf() {
        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.HOME_LOTTIE_JSON,
                "application/pdf"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only application/json");
    }

    @Test
    void homeLottieJsonRejectsOversizedFile() {
        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.HOME_LOTTIE_JSON,
                "application/json",
                2L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 2 MB");
    }

    @Test
    void homeLottieJsonAcceptsFileSizeAtLimit() {
        validator.validateFileSize(DashboardMediaUploadType.HOME_LOTTIE_JSON, "application/json", 2L * 1024 * 1024);
    }

    @Test
    void homeLottieJsonRequiresNoContextId() {
        validator.validateContextIds(DashboardMediaUploadType.HOME_LOTTIE_JSON, null, null, null, null, null);
    }

    // --- Brand uploads (Phase 2B.2) ---

    @Test
    void brandLogoAcceptsSupportedImagesAndRejectsInvalidMime() {
        validator.validateContentType(DashboardMediaUploadType.BRAND_LOGO, "image/jpeg");
        validator.validateContentType(DashboardMediaUploadType.BRAND_LOGO, "image/png");
        validator.validateContentType(DashboardMediaUploadType.BRAND_LOGO, "image/webp");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.BRAND_LOGO,
                "image/svg+xml"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only image/jpeg, image/jpg, image/png, image/webp");
    }

    @Test
    void brandLogoRequiresBrandId() {
        assertThatThrownBy(() -> validator.validateContextIds(
                DashboardMediaUploadType.BRAND_LOGO,
                null,
                null,
                null,
                null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BRAND_LOGO requires brandId");
    }

    @Test
    void brandLogoRejectsOversizedFile() {
        validator.validateFileSize(DashboardMediaUploadType.BRAND_LOGO, "image/png", 2L * 1024 * 1024);

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.BRAND_LOGO,
                "image/png",
                2L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 2 MB");
    }

    @Test
    void brandHeroImageValidatesMimeAndSize() {
        validator.validateContentType(DashboardMediaUploadType.BRAND_HERO_IMAGE, "image/webp");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.BRAND_HERO_IMAGE,
                "application/pdf"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an image content type");

        validator.validateFileSize(DashboardMediaUploadType.BRAND_HERO_IMAGE, "image/webp", 5L * 1024 * 1024);

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.BRAND_HERO_IMAGE,
                "image/webp",
                5L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 5 MB");
    }

    @Test
    void brandSkuImageValidatesImageOnly() {
        validator.validateContentType(DashboardMediaUploadType.BRAND_SKU_IMAGE, "image/jpeg");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.BRAND_SKU_IMAGE,
                "video/mp4"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only image/jpeg, image/jpg, image/png, image/webp");

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.BRAND_SKU_IMAGE,
                "image/jpeg",
                3L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 3 MB");
    }

    @Test
    void brandCertificateFileAllowsPdfAndImageButRejectsVideo() {
        validator.validateContentType(DashboardMediaUploadType.BRAND_CERTIFICATE_FILE, "application/pdf");
        validator.validateContentType(DashboardMediaUploadType.BRAND_CERTIFICATE_FILE, "image/png");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.BRAND_CERTIFICATE_FILE,
                "video/mp4"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only image/jpeg, image/jpg, image/png, image/webp, application/pdf");

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.BRAND_CERTIFICATE_FILE,
                "application/pdf",
                10L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 10 MB");
    }

    @Test
    void brandPromoMediaAllowsImageAndVideoButRejectsUnsupportedFile() {
        validator.validateContentType(DashboardMediaUploadType.BRAND_PROMO_MEDIA, "image/jpeg");
        validator.validateContentType(DashboardMediaUploadType.BRAND_PROMO_MEDIA, "video/mp4");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.BRAND_PROMO_MEDIA,
                "application/pdf"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supports only image/jpeg, image/jpg, image/png, image/webp, video/mp4");
    }

    @Test
    void brandPromoMediaAppliesDifferentSizeLimitsForImageVsVideo() {
        validator.validateFileSize(DashboardMediaUploadType.BRAND_PROMO_MEDIA, "image/jpeg", 5L * 1024 * 1024);

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.BRAND_PROMO_MEDIA,
                "image/jpeg",
                5L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 5 MB");

        validator.validateFileSize(DashboardMediaUploadType.BRAND_PROMO_MEDIA, "video/mp4", 25L * 1024 * 1024);

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.BRAND_PROMO_MEDIA,
                "video/mp4",
                25L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 25 MB");
    }

    @Test
    void brandProductCategoryImageAcceptsSupportedImagesAndRejectsInvalidMime() {
        validator.validateContentType(DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE, "image/jpeg");
        validator.validateContentType(DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE, "image/webp");

        assertThatThrownBy(() -> validator.validateContentType(
                DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE,
                "application/pdf"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an image content type");
    }

    @Test
    void brandProductCategoryImageRequiresBrandId() {
        assertThatThrownBy(() -> validator.validateContextIds(
                DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE,
                null,
                null,
                null,
                null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BRAND_PRODUCT_CATEGORY_IMAGE requires brandId");
    }

    @Test
    void brandProductCategoryImageRejectsOversizedFile() {
        validator.validateFileSize(DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE, "image/png", 2L * 1024 * 1024);

        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE,
                "image/png",
                2L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 2 MB");
    }

    @Test
    void masterPlanRequestRejectsNegativeAreaAndCounts() {
        Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
        ProjectMasterPlanUpsertRequest request = ProjectMasterPlanUpsertRequest.builder()
                .totalUnits(-1)
                .parkAreaValue(new BigDecimal("-0.01"))
                .build();

        assertThat(beanValidator.validate(request))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("totalUnits", "parkAreaValue");
    }
}
