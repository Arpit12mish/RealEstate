package com.brandPitara.sfs.calculator.circlerate.entity;

import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateFormulaType;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRatePropertyType;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateUnitType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "circle_rate_rule",
        indexes = {
                @Index(name = "idx_cr_city", columnList = "city_name"),
                @Index(name = "idx_cr_city_locality", columnList = "city_name, locality_name"),
                @Index(name = "idx_cr_lookup", columnList = "state_name, city_name, locality_name, property_type, active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleRateRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_name", nullable = false, length = 100)
    private String stateName;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Column(name = "locality_name", nullable = false, length = 150)
    private String localityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 40)
    private CircleRatePropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 20)
    private CircleRateUnitType unitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "formula_type", nullable = false, length = 40)
    private CircleRateFormulaType formulaType;

    @Column(name = "rate_per_unit", nullable = false, precision = 18, scale = 2)
    private BigDecimal ratePerUnit;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "source_note", length = 500)
    private String sourceNote;
}