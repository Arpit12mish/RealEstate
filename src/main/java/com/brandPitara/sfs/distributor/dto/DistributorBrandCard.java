package com.brandPitara.sfs.distributor.dto;

// import com.brandPitara.sfs.brand.enums.DistributorBrandStatus; // if you created it
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistributorBrandCard {

  private Long brandId;
  private String brandName;
  private String brandLogoUrl;

  // mapping-specific offer (brand → distributor)
  private String offerTitle;
  private String offerDescription;
  private String offerBannerUrl;
  private OffsetDateTime validTill;

  private Integer priority;
  private Boolean active;
//   private DistributorBrandStatus status; // OPTIONAL (if you have)
}
