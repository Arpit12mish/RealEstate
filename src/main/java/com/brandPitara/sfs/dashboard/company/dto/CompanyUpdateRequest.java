package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

// All fields optional/nullable - only non-null fields are applied (partial update),
// matching BrandCollaborationServiceImpl's applyOptionalFields convention. slug/name/
// companyType, if sent, must still be non-blank (checked in the service, not here,
// since "field omitted" and "field cleared" must be distinguishable at this layer).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyUpdateRequest {

  @Size(max = 150)
  private String name;

  @Size(max = 180)
  private String slug;

  @Size(max = 40)
  private String companyType;

  private String logoUrl;
  private String coverImageUrl;
  private String description;

  @Size(max = 255)
  private String specializationText;

  @Size(max = 20, message = "Maximum 20 services offered")
  private List<@Size(max = 60, message = "Each service must be 60 characters or less") String> servicesOffered;

  private Long cityId;
  private String addressLine;

  @Size(max = 20)
  private String phone;

  @Size(max = 20)
  private String whatsapp;

  @Size(max = 150)
  private String email;

  private Integer priority;
  private Boolean active;
  private Boolean published;
}
