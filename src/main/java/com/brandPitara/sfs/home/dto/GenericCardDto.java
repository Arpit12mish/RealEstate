package com.brandPitara.sfs.home.dto;

import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties("favorite")
public class GenericCardDto {
  private Long id;          // item row id
  private HomeSectionItemType itemType; // BRAND / BUILDER / PROJECT / etc
  private Long refId;       // brandId/builderId/projectId etc
  private String title;
  private String subtitle;
  private String imageUrl;
  private String logoUrl;
  private long favoriteCount;
  @JsonProperty("isFavorite")
  private boolean isFavorite;
}
