package com.brandPitara.sfs.provider.entity;

import com.brandPitara.sfs.entity.CityEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider_service_area")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderServiceAreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfileEntity provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @Column(length = 120)
    private String locality;

    @Column(length = 12)
    private String pincode;

    private Double latitude;
    private Double longitude;
}
