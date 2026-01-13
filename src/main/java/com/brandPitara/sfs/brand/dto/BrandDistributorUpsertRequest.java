package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.entity.BrandDistributorEntity.Status;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandDistributorUpsertRequest {

  @NotNull
  private Long distributorId;

  private Status status;        // ACTIVE/INACTIVE
  private Integer priority;

  private String offerTitle;
  private String offerDescription;
  private String offerBannerUrl;
  private OffsetDateTime validTill;

  private Boolean active;
}
