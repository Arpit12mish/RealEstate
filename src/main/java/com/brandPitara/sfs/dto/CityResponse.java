package com.brandPitara.sfs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CityResponse {
    private Long id;
    private String name;
    private String state;
    private String countryCode;
    private Double latitude;
    private Double longitude;
}
