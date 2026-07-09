package com.brandPitara.sfs.brand.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

class BrandUpsertRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void deserializesExplicitNullPublicStatsFields() throws Exception {
    BrandUpsertRequest request = objectMapper.readValue("""
        {
          "name": "Ikea",
          "foundedYear": null,
          "customerRating": null,
          "customerRatingCount": null
        }
        """, BrandUpsertRequest.class);

    assertThat(request.getName()).isEqualTo("Ikea");
    assertThat(request.getFoundedYear()).isNull();
    assertThat(request.getCustomerRating()).isNull();
    assertThat(request.getCustomerRatingCount()).isNull();
    assertThat(request.isFoundedYearPresent()).isTrue();
    assertThat(request.isCustomerRatingPresent()).isTrue();
    assertThat(request.isCustomerRatingCountPresent()).isTrue();
  }

  @Test
  void acceptsValidPublicStatsFields() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .foundedYear(1943)
        .customerRating(new BigDecimal("4.7"))
        .customerRatingCount(350)
        .build();

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void rejectsFoundedYearBefore1800() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .foundedYear(1799)
        .build();

    assertThat(validator.validate(request)).isNotEmpty();
  }

  @Test
  void rejectsFutureFoundedYear() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Future Brand")
        .foundedYear(Year.now().getValue() + 1)
        .build();

    assertThat(validator.validate(request)).isNotEmpty();
  }

  @Test
  void rejectsCustomerRatingBelowZero() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .customerRating(new BigDecimal("-0.1"))
        .build();

    assertThat(validator.validate(request)).isNotEmpty();
  }

  @Test
  void rejectsCustomerRatingAboveFive() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .customerRating(new BigDecimal("5.1"))
        .build();

    assertThat(validator.validate(request)).isNotEmpty();
  }

  @Test
  void rejectsCustomerRatingWithMoreThanOneDecimalPlace() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .customerRating(new BigDecimal("4.75"))
        .build();

    assertThat(validator.validate(request)).isNotEmpty();
  }

  @Test
  void rejectsNegativeCustomerRatingCount() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .customerRatingCount(-1)
        .build();

    assertThat(validator.validate(request)).isNotEmpty();
  }
}
