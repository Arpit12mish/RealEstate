package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.PromoMediaType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
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

  // Full replace of this brand's category tags (brand_category_link) when present.
  private List<Long> categoryIds;

  private Integer priority;
  private Boolean active;

  // ✅ NEW
  private PromoMediaType promoMediaType; // GIF / LOTTIE
  private String promoMediaUrl;
  private Boolean promoEnabled;
}
