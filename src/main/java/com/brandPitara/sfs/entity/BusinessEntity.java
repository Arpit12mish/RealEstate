package com.brandPitara.sfs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "business",
    indexes = {
        @Index(name = "idx_business_city_id",       columnList = "city_id"),
        @Index(name = "idx_business_category_id",   columnList = "category_id"),
        @Index(name = "idx_business_owner_user_id", columnList = "owner_user_id"),
        @Index(name = "idx_business_active",        columnList = "is_active"),
        @Index(name = "idx_business_sponsored",     columnList = "sponsored,sponsored_priority")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "primary_phone", length = 20)
    private String primaryPhone;

    @Column(name = "secondary_phone", length = 20)
    private String secondaryPhone;

    @Column(name = "whatsapp_phone", length = 20)
    private String whatsappPhone;

    private String email;
    private String website;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String landmark;
    private String pincode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id")
    private CityEntity city;

    private Double latitude;
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(name = "avg_rating", nullable = false)
    @Builder.Default
    private Double avgRating = 0.0;

    @Column(name = "total_ratings", nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // NEW FIELDS FOR CARD

    @Column(name = "open_time")
    private LocalTime openTime;      // e.g. 08:00

    @Column(name = "close_time")
    private LocalTime closeTime;     // e.g. 22:00

    @Column(name = "established_year")
    private Integer establishedYear; // e.g. 2017

    @Column(name = "highlight_badge", length = 150)
    private String highlightBadge;   // "Best selling panasonic TV"

    @Column(name = "is_top_rated", nullable = false)
    @Builder.Default
    private Boolean topRated = false;

    @Column(name = "is_near_and_fast", nullable = false)
    @Builder.Default
    private Boolean nearAndFast = false;
    
    // 🔥 SPONSORED FIELDS

    /**
     * If true, this business can be boosted in listings.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean sponsored = false;

    @Column(name = "sponsored_priority", nullable = false)
    @Builder.Default
    private Integer sponsoredPriority = 0;

    /**
     * Optional validity window for the sponsorship.
     */
    @Column(name = "sponsored_start")
    private OffsetDateTime sponsoredStart;

    @Column(name = "sponsored_end")
    private OffsetDateTime sponsoredEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

}
