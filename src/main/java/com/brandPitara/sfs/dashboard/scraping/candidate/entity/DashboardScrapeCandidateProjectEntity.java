package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "dashboard_scrape_candidate_project",
        indexes = @Index(name = "idx_scrape_cand_project_cid", columnList = "candidate_id")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateProjectEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private DashboardScrapeCandidateEntity candidate;

    @Column(length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "city_name", length = 150)
    private String cityName;

    @Column(name = "address_line", columnDefinition = "text")
    private String addressLine;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "price_min")
    private Long priceMin;

    @Column(name = "price_max")
    private Long priceMax;

    @Column(name = "possession_date")
    private LocalDate possessionDate;

    @Column(name = "rera_number", length = 150)
    private String reraNumber;

    @Column(name = "project_status", length = 80)
    private String projectStatus;

    // Comma-separated PropertyType enum names: "APARTMENT,PLOT"
    @Column(name = "property_types", columnDefinition = "text")
    private String propertyTypes;
}
