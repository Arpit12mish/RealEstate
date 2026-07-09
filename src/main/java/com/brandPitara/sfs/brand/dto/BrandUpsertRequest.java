package com.brandPitara.sfs.brand.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.brandPitara.sfs.brand.enums.PromoMediaType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Getter @Setter
@NoArgsConstructor(onConstructor_ = @JsonCreator) @AllArgsConstructor @Builder
public class BrandUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  private String logoUrl;
  private String description;

  @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase, alphanumeric, hyphen-separated")
  @Size(max = 180)
  private String slug;

  @Size(max = 500)
  private String heroImageUrl;

  @Size(max = 255)
  private String shortDescription;

  @Min(value = 1800, message = "foundedYear must be 1800 or later")
  private Integer foundedYear;
  @JsonIgnore
  @Builder.Default
  private Boolean foundedYearPresent = false;

  @DecimalMin(value = "0.0", message = "customerRating must be at least 0.0")
  @DecimalMax(value = "5.0", message = "customerRating must be at most 5.0")
  @Digits(integer = 1, fraction = 1, message = "customerRating must have at most one decimal place")
  private BigDecimal customerRating;
  @JsonIgnore
  @Builder.Default
  private Boolean customerRatingPresent = false;

  @Min(value = 0, message = "customerRatingCount must be greater than or equal to 0")
  private Integer customerRatingCount;
  @JsonIgnore
  @Builder.Default
  private Boolean customerRatingCountPresent = false;

  // Full replace of this brand's category tags (brand_category_link) when present.
  private List<Long> categoryIds;

  private Integer priority;
  private Boolean active;

  // ✅ NEW
  private PromoMediaType promoMediaType; // GIF / LOTTIE
  private String promoMediaUrl;
  private Boolean promoEnabled;

  @AssertTrue(message = "foundedYear cannot be in the future")
  @JsonIgnore
  public boolean isFoundedYearNotInFuture() {
    return foundedYear == null || foundedYear <= Year.now().getValue();
  }

  public void setFoundedYear(Integer foundedYear) {
    this.foundedYear = foundedYear;
    this.foundedYearPresent = true;
  }

  public void setCustomerRating(BigDecimal customerRating) {
    this.customerRating = customerRating;
    this.customerRatingPresent = true;
  }

  public void setCustomerRatingCount(Integer customerRatingCount) {
    this.customerRatingCount = customerRatingCount;
    this.customerRatingCountPresent = true;
  }

  @JsonIgnore
  public boolean isFoundedYearPresent() {
    return Boolean.TRUE.equals(foundedYearPresent);
  }

  @JsonIgnore
  public boolean isCustomerRatingPresent() {
    return Boolean.TRUE.equals(customerRatingPresent);
  }

  @JsonIgnore
  public boolean isCustomerRatingCountPresent() {
    return Boolean.TRUE.equals(customerRatingCountPresent);
  }
}
