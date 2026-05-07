package com.brandPitara.sfs.dashboard.projectmeter.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardProjectLandUtilizationRequest {

    @DecimalMin(value = "0.0", message = "totalLandAreaSqm must be 0 or greater")
    private Double totalLandAreaSqm;

    @DecimalMin(value = "0.0", message = "commercialAreaSqm must be 0 or greater")
    private Double commercialAreaSqm;

    @DecimalMin(value = "0.0", message = "parksAreaSqm must be 0 or greater")
    private Double parksAreaSqm;

    @DecimalMin(value = "0.0", message = "openAreaSqm must be 0 or greater")
    private Double openAreaSqm;

    @DecimalMin(value = "0.0", message = "residentialAreaSqm must be 0 or greater")
    private Double residentialAreaSqm;

    @DecimalMin(value = "0.0", message = "parkingAreaSqm must be 0 or greater")
    private Double parkingAreaSqm;

    @DecimalMin(value = "0.0", message = "utilityAreaSqm must be 0 or greater")
    private Double utilityAreaSqm;
}