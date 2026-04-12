package com.brandPitara.sfs.calculator.stampduty.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StampDutyCityResponse {
    private String stateName;
    private String cityName;
}