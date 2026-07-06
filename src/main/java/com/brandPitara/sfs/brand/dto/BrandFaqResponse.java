package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandFaqResponse {
  private Long id;
  private Long brandId;
  private String question;
  private String answer;
  private int displayOrder;
  private boolean active;
}
