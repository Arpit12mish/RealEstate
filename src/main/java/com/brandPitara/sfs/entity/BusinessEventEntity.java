package com.brandPitara.sfs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "business_event",
    indexes = {
        @Index(name = "idx_biz_event_business_id",  columnList = "business_id"),
        @Index(name = "idx_biz_event_city_id",      columnList = "city_id"),
        @Index(name = "idx_biz_event_category_id",  columnList = "category_id"),
        @Index(name = "idx_biz_event_created_at",   columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id")
    private BusinessEntity business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private CityEntity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "listing_position")
    private Integer listingPosition;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
