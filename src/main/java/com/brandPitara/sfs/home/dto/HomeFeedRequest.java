package com.brandPitara.sfs.home.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeFeedRequest {
  private Long cityId;
  private Long categoryId;
  private Long builderId;
  private Long clientVersion;
  private Double latitude;
  private Double longitude;
  private Double accuracyMeters;
  private String deviceCity;

  public boolean hasUserCoordinates() {
    return latitude != null && longitude != null;
  }
}
