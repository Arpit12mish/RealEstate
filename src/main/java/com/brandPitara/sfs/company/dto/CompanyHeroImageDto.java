package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyHeroImageDto {
  private Long id;
  private String mediaUrl;
  private String mediaType;
  private String title;
  private String altText;
  private Integer sortOrder;
}
