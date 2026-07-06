package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCollaborationResponse {
  private Long id;
  private Long brandId;
  private BrandCollaborationTargetType targetType;
  private Long targetId;
  private String targetName;
  private String targetLogoUrl;
  private BrandRelationType relationType;
  private BrandSourceType sourceType;
  private String usageCategory;
  private String title;
  private String description;
  private boolean verified;
  private boolean publicVisible;
  private boolean featured;
  private int displayOrder;
  private boolean active;
}
