package com.brandPitara.sfs.distributor.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DistributorResponse {
  private Long id;
  private String name;
  private String phone;
  private String whatsapp;
  private String email;
  private String addressLine1;
  private String addressLine2;
  private String pincode;
  private Long cityId;
  private Double latitude;
  private Double longitude;
  private boolean active;
}
