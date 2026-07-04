package com.brandPitara.sfs.instagram.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardInstagramReelUpsertRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidInstagramUrl() {
        DashboardInstagramReelUpsertRequest request = DashboardInstagramReelUpsertRequest.builder()
            .instagramUrl("https://example.com/video/1")
            .thumbnailUrl("https://cdn.example.com/thumb.webp")
            .build();

        assertThat(validator.validate(request))
            .extracting(v -> v.getPropertyPath().toString())
            .contains("instagramUrl");
    }

    @Test
    void acceptsInstagramReelUrl() {
        DashboardInstagramReelUpsertRequest request = DashboardInstagramReelUpsertRequest.builder()
            .instagramUrl("https://www.instagram.com/reel/ABC123/")
            .thumbnailUrl("https://cdn.example.com/thumb.webp")
            .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}
