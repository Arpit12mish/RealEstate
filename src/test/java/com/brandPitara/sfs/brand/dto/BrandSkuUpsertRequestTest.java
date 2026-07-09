package com.brandPitara.sfs.brand.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrandSkuUpsertRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsHttpsExternalUrl() {
    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .externalUrl("https://berger.com/products/silk-glamour")
        .build();

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void acceptsMissingExternalUrl() {
    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .build();

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void rejectsJavascriptSchemeExternalUrl() {
    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .externalUrl("javascript:alert(1)")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsDataSchemeExternalUrl() {
    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .externalUrl("data:text/html,<script>alert(1)</script>")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsFileSchemeExternalUrl() {
    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .externalUrl("file:///etc/passwd")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsTelSchemeExternalUrl() {
    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .externalUrl("tel:+911234567890")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsMailtoSchemeExternalUrl() {
    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .externalUrl("mailto:someone@example.com")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }
}
