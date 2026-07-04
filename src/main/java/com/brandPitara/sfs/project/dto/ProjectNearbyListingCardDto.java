package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.home.cards.dto.CardActionDto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectNearbyListingCardDto {
  private Long projectId;
  private String projectName;
  private String projectSlug;
  private String coverImageUrl;
  private String addressLine;
  private String cityName;
  private String localityName;
  private Long builderId;
  private String builderName;
  private String builderLogoUrl;
  private Long priceMin;
  private Long priceMax;
  private String priceLabel;
  private String unitSummary;
  private Double latitude;
  private Double longitude;
  private Double distanceKm;
  private String distanceLabel;
  private Boolean verified;
  @Builder.Default
  private Long favoriteCount = 0L;
  @Builder.Default
  private Boolean isFavorite = false;
  private CardActionDto action;
}
