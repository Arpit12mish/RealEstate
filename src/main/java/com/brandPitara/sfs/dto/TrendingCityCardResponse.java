package com.brandPitara.sfs.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingCityCardResponse {
    private Long id;
    private String name;
    private String slug;
    private String state;
    private String countryCode;
    private String coverImageUrl;
    private Long projectCount;
    private Double growthPercent;
    private Integer displayOrder;
    private Boolean comingSoon;
}
