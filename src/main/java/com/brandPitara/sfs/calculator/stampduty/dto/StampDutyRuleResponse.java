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
public class StampDutyRuleResponse {

    private Long id;
    private String stateName;
    private String cityName;
    private StampDutyBuyerType buyerType;
    private StampDutyPropertyCategory propertyCategory;
    private BigDecimal stampDutyPercent;
    private BigDecimal registrationPercent;
    private BigDecimal localBodyTaxPercent;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private String sourceNote;
}