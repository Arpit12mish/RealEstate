package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCertificateResponse {
  private Long id;
  private Long brandId;
  private String title;
  private String issuer;
  private String certificateUrl;
  private int displayOrder;
  private boolean active;
}
