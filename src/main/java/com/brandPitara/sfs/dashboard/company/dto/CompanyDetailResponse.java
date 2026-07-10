package com.brandPitara.sfs.dashboard.company.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyDetailResponse {
  private Long id;
  private String name;
  private String slug;
  private String companyType;
  private String logoUrl;
  private String coverImageUrl;
  private String description;
  private String specializationText;
  private List<String> servicesOffered;
  private Long cityId;
  private String cityName;
  private String addressLine;
  private String phone;
  private String whatsapp;
  private String email;
  private int priority;
  private boolean active;
  private boolean published;
  private boolean deleted;
}
