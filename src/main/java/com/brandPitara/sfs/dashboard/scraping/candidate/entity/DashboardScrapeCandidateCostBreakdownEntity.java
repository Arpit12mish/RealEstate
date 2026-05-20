package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dashboard_scrape_candidate_cost_breakdown")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateCostBreakdownEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private DashboardScrapeCandidateEntity candidate;

    @Column(name = "land_cost")
    private Long landCost;

    @Column(name = "construction_cost")
    private Long constructionCost;

    @Column(name = "infrastructure_cost")
    private Long infrastructureCost;

    @Column(name = "other_cost")
    private Long otherCost;

    @Column(name = "total_cost")
    private Long totalCost;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(nullable = false)
    private boolean verified;
}
