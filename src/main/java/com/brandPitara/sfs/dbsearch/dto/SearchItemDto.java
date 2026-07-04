package com.brandPitara.sfs.dbsearch.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties("favorite")
public class SearchItemDto {
    private Long id;
    private SearchEntityType entityType;

    private String title;
    private String subtitle;

    private String imageUrl;
    private String slug;

    private Long cityId;
    private String cityName;

    private Long builderId;
    private String builderName;

    private String companyType;

    private String location;
    private Long priceMin;
    private Long priceMax;
    private String priceLabel;
    private LocalDate projectStartDate;
    private LocalDate startedOn;
    private List<String> tags;
    private long favoriteCount;
    @JsonProperty("isFavorite")
    private boolean isFavorite;
}
