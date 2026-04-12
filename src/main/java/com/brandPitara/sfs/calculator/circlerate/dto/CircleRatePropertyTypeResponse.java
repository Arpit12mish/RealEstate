package com.brandPitara.sfs.calculator.circlerate.dto;

import com.brandPitara.sfs.calculator.circlerate.enums.CircleRatePropertyType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleRatePropertyTypeResponse {
    private CircleRatePropertyType propertyType;
    private String label;
}