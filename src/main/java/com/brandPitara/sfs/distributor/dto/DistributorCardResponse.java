package com.brandPitara.sfs.distributor.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DistributorCardResponse {
  private Long id;
  private String name;

  private String phone;
  private String whatsapp;

  private Long cityId;

  // brand-mapping specific
  private Integer priority;
  private String offerTitle;
  private String offerBannerUrl;
  private OffsetDateTime validTill;
}
