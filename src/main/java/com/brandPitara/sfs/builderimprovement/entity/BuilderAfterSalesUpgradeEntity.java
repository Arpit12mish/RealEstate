package com.brandPitara.sfs.builderimprovement.entity;

import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementEvidenceStatus;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementImpactLevel;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "builder_after_sales_upgrade",
        indexes = {
                @Index(name = "idx_builder_after_sales_upgrade_profile", columnList = "profile_id"),
                @Index(name = "idx_builder_after_sales_upgrade_public_sort", columnList = "profile_id, published, active, deleted, display_order, id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderAfterSalesUpgradeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent improvement profile.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private BuilderImprovementProfileEntity profile;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "short_summary", columnDefinition = "text")
    private String shortSummary;

    @Column(name = "icon_key", length = 80)
    private String iconKey;

    @Column(name = "legacy_issue_title", length = 180)
    private String legacyIssueTitle;

    @Column(name = "legacy_issue_description", columnDefinition = "text")
    private String legacyIssueDescription;

    @Column(name = "solution_title", length = 180)
    private String solutionTitle;

    @Column(name = "solution_description", columnDefinition = "text")
    private String solutionDescription;

    @Column(name = "result_title", length = 180)
    private String resultTitle;

    @Column(name = "result_description", columnDefinition = "text")
    private String resultDescription;

    @Column(name = "metric_value", length = 80)
    private String metricValue;

    @Column(name = "metric_label", length = 120)
    private String metricLabel;

    @Column(name = "implemented_at")
    private LocalDate implementedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "impact_level", nullable = false, length = 30)
    private BuilderImprovementImpactLevel impactLevel = BuilderImprovementImpactLevel.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", nullable = false, length = 40)
    private BuilderImprovementEvidenceStatus evidenceStatus = BuilderImprovementEvidenceStatus.EVIDENCE_PENDING;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "published", nullable = false)
    private Boolean published = true;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}