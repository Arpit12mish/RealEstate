package com.brandPitara.sfs.calculator.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculatorActionDto {
    private String type;
    private String target;
    private String path;
}
