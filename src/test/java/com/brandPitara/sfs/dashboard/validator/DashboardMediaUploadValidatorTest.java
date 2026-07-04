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
                null
        ))
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
                null
        ))
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
                2L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 2 MB");
    }

    @Test
    void homeLottieJsonAcceptsFileSizeAtLimit() {
        validator.validateFileSize(DashboardMediaUploadType.HOME_LOTTIE_JSON, 2L * 1024 * 1024);
    }

    @Test
    void homeLottieJsonRequiresNoContextId() {
        validator.validateContextIds(DashboardMediaUploadType.HOME_LOTTIE_JSON, null, null, null);
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
