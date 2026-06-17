package com.brandPitara.sfs.dashboard.validator;

import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import org.junit.jupiter.api.Test;

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
    void cityCoverImageRejectsOversizedFile() {
        assertThatThrownBy(() -> validator.validateFileSize(
                DashboardMediaUploadType.CITY_COVER_IMAGE,
                2L * 1024 * 1024 + 1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size of 2 MB");
    }
}
