package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCollaborationUpsertRequest {

  @NotNull
  private BrandCollaborationTargetType targetType;

  @NotNull
  private Long targetId;

  @NotNull
  private BrandRelationType relationType;

  // Optional - defaults to ADMIN_ENTERED on create if omitted.
  private BrandSourceType sourceType;

  @Size(max = 150)
  private String usageCategory;

  @Size(max = 255)
  private String title;

  @Size(max = 4000)
  private String description;

  private Boolean verified;
  private Boolean publicVisible;
  private Boolean featured;

  @Min(0) @Max(9999)
  private Integer displayOrder;

  private Boolean active;
}
