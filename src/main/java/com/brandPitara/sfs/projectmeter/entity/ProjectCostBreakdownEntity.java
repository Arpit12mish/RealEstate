package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_cost_breakdown")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCostBreakdownEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private ProjectEntity project;

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

    @Column(name = "source_label", length = 80)
    private String sourceLabel;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
}