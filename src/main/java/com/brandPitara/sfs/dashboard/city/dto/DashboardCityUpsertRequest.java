package com.brandPitara.sfs.dashboard.city.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardCityUpsertRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

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
}
