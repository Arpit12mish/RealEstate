package com.brandPitara.sfs.brand.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandSkuUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase, alphanumeric, hyphen-separated")
  @Size(max = 180)
  private String slug;

  @Size(max = 80)
  private String skuCode;

  private Long categoryId;

  // The brand's own product category (Lamps/Mirrors/Kitchenware) - distinct from categoryId
  // above, which is the global taxonomy. Must belong to the same brand as this SKU.
  private Long productCategoryId;

  @Size(max = 255)
  private String shortDescription;

  @Size(max = 4000)
  private String description;

  @Size(max = 500)
  private String imageUrl;

  @Size(max = 80)
  private String priceLabel;

  // The product detail page on the brand's own external website.
  @Pattern(regexp = "^https?://.+", message = "externalUrl must be a valid http/https URL")
  @Size(max = 500)
  private String externalUrl;

  private Boolean featured;
  private Boolean latest;

  @Min(0) @Max(9999)
  private Integer displayOrder;

  private Boolean published;
  private Boolean active;
}
