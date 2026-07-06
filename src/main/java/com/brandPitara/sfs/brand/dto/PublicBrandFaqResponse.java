package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandFaqResponse {
  private Long id;
  private String question;
  private String answer;
  private int displayOrder;
}
