package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.BrandRelationType;
import lombok.*;

/**
 * Not in the Phase 1C required DTO list - added because the brand detail page's
 * topProjects/topBuilders/topArchitects/interiorDesigners sections need a summary of the
 * *target* (project/builder/company), which is the opposite direction from
 * PublicBrandConnectedResponse (a brand, as seen from a project/builder's page).
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicCollaborationTargetSummary {
  private Long id;
  private String name;
  private String logoUrl;
  private BrandRelationType relationType;
  private boolean verified;
  private boolean featured;
}
