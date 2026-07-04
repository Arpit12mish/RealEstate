package com.brandPitara.sfs.builder.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties("favorite")
public class BuilderProjectCardDto {
  private Long id;
  private String name;

  private Long cityId;
  private String cityName;

  private String addressLine;

  private Long priceMin;
  private Long priceMax;

  private Long builderId;
  private String builderName;
  private String builderLogoUrl;

  private String coverMediaUrl;
  private String coverMediaType; // IMAGE/VIDEO (string)

  private LocalDate projectStartDate;
  private LocalDate startedOn;

  private long favoriteCount;
  @JsonProperty("isFavorite")
  private boolean isFavorite;
}
