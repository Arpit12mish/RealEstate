package com.brandPitara.sfs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CityResponse {
    private Long id;
    private String name;
    private String slug;
    private String state;
    private String countryCode;
    private Double latitude;
    private Double longitude;
    private String coverImageUrl;
    private Boolean active;
    private Boolean homepageFeatured;
    private Integer displayOrder;
    private Double growthPercent;
}
