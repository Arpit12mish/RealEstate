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
public class StampDutyCalculateRequest {

    @NotBlank(message = "State name is required")
    private String stateName;

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotNull(message = "Buyer type is required")
    private StampDutyBuyerType buyerType;

    @NotNull(message = "Property category is required")
    private StampDutyPropertyCategory propertyCategory;

    @NotNull(message = "Property value is required")
    @DecimalMin(value = "0.01", message = "Property value must be greater than 0")
    private BigDecimal propertyValue;

    private LocalDate asOfDate;
}