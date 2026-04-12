package com.brandPitara.sfs.calculator.interiorcost.dto;

import com.brandPitara.sfs.calculator.interiorcost.enums.*;
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
public class InteriorCostAddonRuleUpsertRequest {

    @NotNull(message = "Company id is required")
    private Long companyId;

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotNull(message = "Package type is required")
    private InteriorPackageType packageType;

    @NotNull(message = "Addon type is required")
    private InteriorAddonType addonType;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @NotNull(message = "Effective from is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @NotNull(message = "Active flag is required")
    private Boolean active;

    private String sourceNote;
}