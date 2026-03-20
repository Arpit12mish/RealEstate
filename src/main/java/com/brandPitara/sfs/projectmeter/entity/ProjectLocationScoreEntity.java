package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_location_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLocationScoreEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private ProjectEntity project;

    @Column(name = "metro_score")
    private Double metroScore;

    @Column(name = "education_score")
    private Double educationScore;

    @Column(name = "healthcare_score")
    private Double healthcareScore;

    @Column(name = "retail_score")
    private Double retailScore;

    @Column(name = "job_score")
    private Double jobScore;

    @Column(name = "leisure_score")
    private Double leisureScore;

    @Column(name = "current_strength_score")
    private Double currentStrengthScore;

    @Column(name = "future_growth_score")
    private Double futureGrowthScore;

    @Column(name = "final_score")
    private Double finalScore;

    @Column(name = "appreciation_percent_3y")
    private Double appreciationPercent3Y;

    @Column(name = "score_summary", columnDefinition = "text")
    private String scoreSummary;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
}