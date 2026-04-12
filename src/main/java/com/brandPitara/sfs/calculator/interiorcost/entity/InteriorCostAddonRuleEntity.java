package com.brandPitara.sfs.calculator.interiorcost.entity;

import com.brandPitara.sfs.calculator.interiorcost.enums.InteriorAddonType;
import com.brandPitara.sfs.calculator.interiorcost.enums.InteriorPackageType;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "interior_cost_addon_rule",
        indexes = {
                @Index(name = "idx_icar_city", columnList = "city_name"),
                @Index(name = "idx_icar_lookup", columnList = "city_name, package_type, addon_type, active"),
                @Index(name = "idx_icar_company", columnList = "company_id, active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorCostAddonRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 40)
    private InteriorPackageType packageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "addon_type", nullable = false, length = 40)
    private InteriorAddonType addonType;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "source_note", length = 500)
    private String sourceNote;
}