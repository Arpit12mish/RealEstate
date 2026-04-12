package com.brandPitara.sfs.calculator.stampduty.dto;

import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyPropertyCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StampDutyPropertyCategoryResponse {
    private StampDutyPropertyCategory propertyCategory;
    private String label;
}