package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandCertificateResponse {
  private Long id;
  private String title;
  private String issuer;
  private String certificateUrl;
  private int displayOrder;
}
