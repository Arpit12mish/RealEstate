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

    @Column(length = 150)
    private String state;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode = "IN";

    private Double latitude;
    private Double longitude;
}
