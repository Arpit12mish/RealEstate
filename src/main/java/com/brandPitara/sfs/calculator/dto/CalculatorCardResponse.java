package com.brandPitara.sfs.calculator.dto;

import com.brandPitara.sfs.calculator.enums.CalculatorType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculatorCardResponse {
    private CalculatorType calculatorType;
    private String title;
    private String subtitle;
    private String iconKey;
    private String route;
    private CalculatorActionDto action;
    private Integer displayOrder;
    private Boolean active;
}
