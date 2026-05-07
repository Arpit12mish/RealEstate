package com.brandPitara.sfs.dashboard.projectmeter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardProjectPriceHistoryRequest {

    @NotBlank
    @Size(max = 20)
    @Pattern(
        regexp = "^[0-9]{4}(-[0-9]{2,4})?$",
        message = "Year label must be in format YYYY or YYYY-YY or YYYY-YYYY"
    )
    private String yearLabel;

    @NotNull
    @Min(value = 0, message = "projectPrice must be 0 or greater")
    private Long projectPrice;

    @Min(value = 0, message = "averageAreaPrice must be 0 or greater")
    private Long averageAreaPrice;

    @NotNull
    @Min(0) @Max(999)
    private Integer displayOrder;

    private Boolean verified;
}