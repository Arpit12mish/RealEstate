package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "project_meter_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterSnapshotEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private ProjectEntity project;

    @Column(name = "construction_progress_percent", nullable = false)
    @Builder.Default
    private Integer constructionProgressPercent = 0;

    @Column(name = "delay_days", nullable = false)
    @Builder.Default
    private Integer delayDays = 0;

    @Column(name = "construction_start_date")
    private LocalDate constructionStartDate;

    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Column(name = "revised_completion_date")
    private LocalDate revisedCompletionDate;

    @Column(name = "compliance_score")
    private Integer complianceScore;

    @Column(name = "amenity_score")
    private Integer amenityScore;

    @Column(name = "location_score")
    private Double locationScore;

    @Column(name = "location_appreciation_percent_3y")
    private Double locationAppreciationPercent3Y;

    @Column(name = "launch_price")
    private Long launchPrice;

    @Column(name = "current_price")
    private Long currentPrice;

    @Column(name = "average_area_price")
    private Long averageAreaPrice;

    @Column(name = "price_appreciation_percent")
    private Double priceAppreciationPercent;

    @Column(name = "estimated_cost_total")
    private Long estimatedCostTotal;

    @Column(name = "computed_at")
    private OffsetDateTime computedAt;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
}