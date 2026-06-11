package com.brandPitara.sfs.dbsearch.dto;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private List<String> tags;
}