package com.brandPitara.sfs.calculator.circlerate.dto;

import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateFormulaType;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRatePropertyType;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateUnitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleRateUpsertRequest {

    @NotBlank(message = "State name is required")
    private String stateName;

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotBlank(message = "Locality name is required")
    private String localityName;

    @NotNull(message = "Property type is required")
    private CircleRatePropertyType propertyType;

    @NotNull(message = "Unit type is required")
    private CircleRateUnitType unitType;

    @NotNull(message = "Formula type is required")
    private CircleRateFormulaType formulaType;

    @NotNull(message = "Rate per unit is required")
    @DecimalMin(value = "0.0001", message = "Rate per unit must be greater than 0")
    private BigDecimal ratePerUnit;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @NotNull(message = "Active flag is required")
    private Boolean active;

    private String sourceNote;
}