package com.brandPitara.sfs.brand.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrandProductCategoryUpsertRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsHttpsExternalUrl() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .externalUrl("https://berger.com/lamps")
        .build();

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void acceptsMissingExternalUrl() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .build();

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void rejectsJavascriptSchemeExternalUrl() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .externalUrl("javascript:alert(1)")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsDataSchemeExternalUrl() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .externalUrl("data:text/html,<script>alert(1)</script>")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsFileSchemeExternalUrl() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .externalUrl("file:///etc/passwd")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsTelSchemeExternalUrl() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .externalUrl("tel:+911234567890")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsMailtoSchemeExternalUrl() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .externalUrl("mailto:someone@example.com")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("externalUrl");
  }

  @Test
  void rejectsBlankName() {
    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("")
        .build();

    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("name");
  }
}
