package com.brandPitara.sfs.calculator.stampduty.dto;

import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyBuyerType;
import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyPropertyCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StampDutyCalculateResponse {

    private String stateName;
    private String cityName;
    private StampDutyBuyerType buyerType;
    private StampDutyPropertyCategory propertyCategory;

    private BigDecimal propertyValue;

    private BigDecimal stampDutyPercent;
    private BigDecimal registrationPercent;
    private BigDecimal localBodyTaxPercent;

    private BigDecimal stampDutyAmount;
    private BigDecimal registrationAmount;
    private BigDecimal localBodyTaxAmount;
    private BigDecimal totalCharges;

    private String currency;
    private String displayMessage;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}