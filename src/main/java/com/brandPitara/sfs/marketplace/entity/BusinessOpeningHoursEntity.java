package com.brandPitara.sfs.marketplace.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * One opening interval on an ISO day of week (1 = Monday). closesAt before opensAt is an
 * overnight interval; closesAt equal to opensAt is a full 24 hours. A day with no rows is closed.
 */
@Entity
@Table(name = "business_opening_hours")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessOpeningHoursEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;
}
