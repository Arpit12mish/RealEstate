package com.brandPitara.sfs.calculator.stampduty.dto;

import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyBuyerType;
import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyPropertyCategory;
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
public class StampDutyUpsertRequest {

    @NotBlank(message = "State name is required")
    private String stateName;

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotNull(message = "Buyer type is required")
    private StampDutyBuyerType buyerType;

    @NotNull(message = "Property category is required")
    private StampDutyPropertyCategory propertyCategory;

    @NotNull(message = "Stamp duty percent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Stamp duty percent cannot be negative")
    private BigDecimal stampDutyPercent;

    @NotNull(message = "Registration percent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Registration percent cannot be negative")
    private BigDecimal registrationPercent;

    @NotNull(message = "Local body tax percent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Local body tax percent cannot be negative")
    private BigDecimal localBodyTaxPercent;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @NotNull(message = "Active flag is required")
    private Boolean active;

    private String sourceNote;
}