package com.brandPitara.sfs.distributor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DistributorUpsertRequest {

  @NotBlank @Size(max=200)
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

  private Boolean active;
}
