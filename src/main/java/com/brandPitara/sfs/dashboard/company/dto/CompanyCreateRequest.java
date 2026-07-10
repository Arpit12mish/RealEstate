package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyCreateRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  // Optional - auto-generated from name if blank (matches BrandServiceImpl's convention).
  @Size(max = 180)
  private String slug;

  @NotBlank
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
