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

  // Additive - only populated for topArchitects/interiorDesigners (company targets), so
  // those cards can match the Home screen's ArchitectDesignerCardDto card exactly.
  // topProjects/topBuilders targets leave these null - the mobile card already treats
  // them as optional and hides the corresponding UI when absent.
  private String coverImageUrl;
  private String description;
  private String subtitle;
  private String specializationText;
  private String yearsExperience;
  private String awardsCount;
  private String citiesServed;
}
