package com.brandPitara.sfs.calculator.interiorcost.dto;

import com.brandPitara.sfs.calculator.interiorcost.enums.InteriorPackageType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorPackageSummaryItemDto {

    private InteriorPackageType packageType;
    private BigDecimal minRatePerUnit;
    private BigDecimal maxRatePerUnit;
    private String areaUnit;
    private String currency;
    private String matchType; // EXACT or APPROXIMATE
    private BigDecimal matchedMinArea;
    private BigDecimal matchedMaxArea;
    private String displayLabel;
}