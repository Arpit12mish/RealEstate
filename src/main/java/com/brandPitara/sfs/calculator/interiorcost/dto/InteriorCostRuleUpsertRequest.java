package com.brandPitara.sfs.calculator.interiorcost.dto;

import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorCostRuleUpsertRequest {

    @NotNull(message = "Company id is required")
    private Long companyId;

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotNull(message = "Property type is required")
    private InteriorPropertyType propertyType;

    @NotNull(message = "Area unit is required")
    private InteriorAreaUnitType areaUnit;

    @NotNull(message = "BHK type is required")
    private InteriorBhkType bhkType;

    @NotNull(message = "Package type is required")
    private InteriorPackageType packageType;

    @NotNull(message = "Scope type is required")
    private InteriorScopeType scopeType;

    @NotNull(message = "Min area is required")
    @DecimalMin(value = "0.01", message = "Min area must be greater than 0")
    private BigDecimal minArea;

    @NotNull(message = "Max area is required")
    @DecimalMin(value = "0.01", message = "Max area must be greater than 0")
    private BigDecimal maxArea;

    @NotNull(message = "Base rate per unit is required")
    @DecimalMin(value = "0.01", message = "Base rate per unit must be greater than 0")
    private BigDecimal baseRatePerUnit;

    @NotNull(message = "Minimum project cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum project cost cannot be negative")
    private BigDecimal minimumProjectCost;

    @NotNull(message = "Contingency percent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Contingency percent cannot be negative")
    private BigDecimal contingencyPercent;

    @NotNull(message = "Tax percent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Tax percent cannot be negative")
    private BigDecimal taxPercent;

    @NotNull(message = "Effective from is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @NotNull(message = "Active flag is required")
    private Boolean active;

    private String sourceNote;
}