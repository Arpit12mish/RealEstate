package com.brandPitara.sfs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "city")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(length = 150)
    private String state;

    @Column(name = "country_code", nullable = false, length = 10)
    @Builder.Default
    private String countryCode = "IN";

    private Double latitude;
    private Double longitude;

    @Column(name = "cover_image_url", columnDefinition = "text")
    private String coverImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "homepage_featured", nullable = false)
    @Builder.Default
    private Boolean homepageFeatured = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "growth_percent")
    private Double growthPercent;
}
