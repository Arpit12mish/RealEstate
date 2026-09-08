package com.brandPitara.sfs.dashboard.promobanner.dto;

import com.brandPitara.sfs.enums.PromoBannerMediaType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardPromoBannerUpsertRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void blankMediaUrlIsRejected() {
        DashboardPromoBannerUpsertRequest request = validRequest();
        request.setMediaUrl(" ");

        assertThat(validator.validate(request))
                .anyMatch(violation -> "mediaUrl".equals(violation.getPropertyPath().toString()));
    }

    @Test
    void requiredMediaTypeIsRejectedWhenMissing() {
        DashboardPromoBannerUpsertRequest request = validRequest();
        request.setMediaType(null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> "mediaType".equals(violation.getPropertyPath().toString()));
    }

    private DashboardPromoBannerUpsertRequest validRequest() {
        DashboardPromoBannerUpsertRequest request = new DashboardPromoBannerUpsertRequest();
        request.setCategoryId(0L);
        request.setSlotKey("HERO");
        request.setTitle("Hero");
        request.setMediaType(PromoBannerMediaType.VIDEO);
        request.setMediaUrl("https://cdn.example.com/hero.mp4");
        return request;
    }
}
