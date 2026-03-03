package com.brandPitara.sfs.home.dto;

import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyItemDto {
  private HomeSectionItemType entityType; // BRAND / BUILDER

  private Long id;        // brandId OR builderId
  private String name;
  private String logoUrl;

  // optional future:
  private String subtitle;
}
