package com.brandPitara.sfs.calculator.stampduty.entity;

import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyBuyerType;
import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyPropertyCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "stamp_duty_rule",
        indexes = {
                @Index(name = "idx_sd_city", columnList = "city_name"),
                @Index(name = "idx_sd_lookup", columnList = "state_name, city_name, buyer_type, property_category, active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StampDutyRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_name", nullable = false, length = 100)
    private String stateName;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "buyer_type", nullable = false, length = 30)
    private StampDutyBuyerType buyerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_category", nullable = false, length = 30)
    private StampDutyPropertyCategory propertyCategory;

    @Column(name = "stamp_duty_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal stampDutyPercent;

    @Column(name = "registration_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal registrationPercent;

    @Column(name = "local_body_tax_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal localBodyTaxPercent;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "source_note", length = 500)
    private String sourceNote;
}
