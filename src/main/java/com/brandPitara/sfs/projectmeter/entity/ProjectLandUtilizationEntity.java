package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_land_utilization")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLandUtilizationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private ProjectEntity project;

    @Column(name = "total_land_area_sqm")
    private Double totalLandAreaSqm;

    @Column(name = "commercial_area_sqm")
    private Double commercialAreaSqm;

    @Column(name = "parks_area_sqm")
    private Double parksAreaSqm;

    @Column(name = "open_area_sqm")
    private Double openAreaSqm;

    @Column(name = "residential_area_sqm")
    private Double residentialAreaSqm;

    @Column(name = "parking_area_sqm")
    private Double parkingAreaSqm;

    @Column(name = "utility_area_sqm")
    private Double utilityAreaSqm;
}