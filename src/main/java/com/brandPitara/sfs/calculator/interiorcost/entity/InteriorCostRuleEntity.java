package com.brandPitara.sfs.calculator.interiorcost.entity;

import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "interior_cost_rule",
        indexes = {
                @Index(name = "idx_icr_city", columnList = "city_name"),
                @Index(name = "idx_icr_lookup", columnList = "city_name, property_type, package_type, scope_type, bhk_type, active"),
                @Index(name = "idx_icr_company", columnList = "company_id, active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorCostRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 40)
    private InteriorPropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_unit", nullable = false, length = 20)
    private InteriorAreaUnitType areaUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "bhk_type", nullable = false, length = 40)
    private InteriorBhkType bhkType;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 40)
    private InteriorPackageType packageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 40)
    private InteriorScopeType scopeType;

    @Column(name = "min_area", nullable = false, precision = 18, scale = 2)
    private BigDecimal minArea;

    @Column(name = "max_area", nullable = false, precision = 18, scale = 2)
    private BigDecimal maxArea;

    @Column(name = "base_rate_per_unit", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseRatePerUnit;

    @Column(name = "minimum_project_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal minimumProjectCost;

    @Column(name = "contingency_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal contingencyPercent;

    @Column(name = "tax_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxPercent;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "source_note", length = 500)
    private String sourceNote;
}