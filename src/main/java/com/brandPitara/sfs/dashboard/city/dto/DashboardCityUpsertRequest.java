package com.brandPitara.sfs.dashboard.city.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardCityUpsertRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 180)
    private String slug;

    @Size(max = 150)
    private String state;

    @Size(max = 10)
    private String countryCode;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @Size(max = 500)
    private String coverImageUrl;

    private Boolean active;
    private Boolean homepageFeatured;

    @Min(0)
    @Max(9999)
    private Integer displayOrder;

    @DecimalMin(value = "-100.0")
    @DecimalMax(value = "9999.0")
    private Double growthPercent;
}
