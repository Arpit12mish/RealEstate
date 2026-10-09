package com.brandPitara.sfs.provider.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.provider.enums.ProviderRateType;
import com.brandPitara.sfs.provider.enums.ProviderRateUnit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "provider_rate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderRateEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfileEntity provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 30)
    private ProviderRateType rateType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** ISO 4217 code. */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderRateUnit unit;

    @Column(length = 200)
    private String note;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
