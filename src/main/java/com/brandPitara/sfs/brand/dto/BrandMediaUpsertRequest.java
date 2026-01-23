package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.entity.BrandMediaEntity.MediaType;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandMediaUpsertRequest {

  @NotNull
  private MediaType mediaType;     // IMAGE / VIDEO

  @NotNull
  private Placement placement;     // BANNER / HERO / GALLERY

  @NotBlank
  private String url;

  private String caption;
  private Integer sortOrder;
  private Boolean active;

  // optional CTA
  private String actionType;   // e.g. "OPEN_URL", "OPEN_DISTRIBUTORS"
  private String actionValue;  // e.g. "https://..."
}
