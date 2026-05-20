package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "dashboard_scrape_candidate_land_utilization")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateLandUtilizationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private DashboardScrapeCandidateEntity candidate;

    @Column(name = "total_land_area_sqm", precision = 14, scale = 2)
    private BigDecimal totalLandAreaSqm;

    @Column(name = "residential_area_sqm", precision = 14, scale = 2)
    private BigDecimal residentialAreaSqm;

    @Column(name = "commercial_area_sqm", precision = 14, scale = 2)
    private BigDecimal commercialAreaSqm;

    @Column(name = "parks_area_sqm", precision = 14, scale = 2)
    private BigDecimal parksAreaSqm;

    @Column(name = "open_area_sqm", precision = 14, scale = 2)
    private BigDecimal openAreaSqm;

    @Column(name = "parking_area_sqm", precision = 14, scale = 2)
    private BigDecimal parkingAreaSqm;

    @Column(name = "utility_area_sqm", precision = 14, scale = 2)
    private BigDecimal utilityAreaSqm;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(nullable = false)
    private boolean verified;
}
