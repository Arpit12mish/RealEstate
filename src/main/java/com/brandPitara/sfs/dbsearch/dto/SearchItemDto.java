package com.brandPitara.sfs.dbsearch.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
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

    private Long projectId;
    private String projectName;

    private String title;
    private String subtitle;

    private String imageUrl;
    private String coverImageUrl;
    private String slug;
    private String projectSlug;

    private Long cityId;
    private String cityName;
    private String citySlug;

    private Long builderId;
    private String builderName;
    private String builderLogoUrl;

    private String companyType;

    private String location;
    private String addressLine;
    private Long priceMin;
    private Long priceMax;
    private String priceLabel;
    private LocalDate projectStartDate;
    private LocalDate startedOn;
    private LocalDate possessionDate;
    private ProjectStatus status;
    private Set<PropertyType> propertyTypes;
    private Integer constructionProgressPercent;
    private Double appreciationPercent;
    private String reraNumber;
    private List<String> tags;
    private long favoriteCount;
    @JsonProperty("isFavorite")
    private boolean isFavorite;
}
