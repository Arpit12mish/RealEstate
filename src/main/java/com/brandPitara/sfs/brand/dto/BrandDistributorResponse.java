package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.entity.BrandDistributorEntity.Status;
import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandDistributorResponse {
  private Long id;
  private Long brandId;
  private Long distributorId;

  private Status status;
  private int priority;

  private String offerTitle;
  private String offerDescription;
  private String offerBannerUrl;
  private OffsetDateTime validTill;

  private boolean active;

  // useful for UI lists
  private String distributorName;
  private String distributorPhone;
  private Long cityId;
}
