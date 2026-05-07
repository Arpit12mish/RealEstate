package com.brandPitara.sfs.calculator.interiorcost.dto;

import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class InteriorCostCustomCompareRequest {

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotNull(message = "Property type is required")
    private InteriorPropertyType propertyType;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.01", message = "Area must be greater than 0")
    private BigDecimal area;

    @NotNull(message = "Area unit is required")
    private InteriorAreaUnitType areaUnit;

    @NotNull(message = "BHK type is required")
    private InteriorBhkType bhkType;

    @NotNull(message = "Package type is required")
    private InteriorPackageType packageType;

    @NotNull(message = "Scope type is required")
    private InteriorScopeType scopeType;

    @Min(value = 0, message = "Kitchen count cannot be negative")
    @Builder.Default
    private Integer kitchenCount = 0;

    @Min(value = 0, message = "Wardrobe count cannot be negative")
    @Builder.Default
    private Integer wardrobeCount = 0;

    @Min(value = 0, message = "Bathroom count cannot be negative")
    @Builder.Default
    private Integer bathroomCount = 0;

    @NotNull(message = "Custom rate per unit is required")
    @DecimalMin(value = "0.01", message = "Custom rate per unit must be greater than 0")
    private BigDecimal customRatePerUnit;

    @NotNull(message = "Custom kitchen cost is required")
    @DecimalMin(value = "0.00", message = "Custom kitchen cost cannot be negative")
    private BigDecimal customKitchenCost;

    @NotNull(message = "Custom wardrobe cost is required")
    @DecimalMin(value = "0.00", message = "Custom wardrobe cost cannot be negative")
    private BigDecimal customWardrobeCost;

    @NotNull(message = "Custom bathroom cost is required")
    @DecimalMin(value = "0.00", message = "Custom bathroom cost cannot be negative")
    private BigDecimal customBathroomCost;

    @NotNull(message = "Custom contingency percent is required")
    @DecimalMin(value = "0.00", message = "Custom contingency percent cannot be negative")
    private BigDecimal customContingencyPercent;

    @NotNull(message = "Custom tax percent is required")
    @DecimalMin(value = "0.00", message = "Custom tax percent cannot be negative")
    private BigDecimal customTaxPercent;

    private LocalDate asOfDate;
}