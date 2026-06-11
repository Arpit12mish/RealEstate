package com.brandPitara.sfs.builderimprovement.entity;

import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementEvidenceStatus;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementIssueStatus;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementIssueType;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "builder_improvement_issue",
        indexes = {
                @Index(name = "idx_builder_improvement_issue_profile", columnList = "profile_id"),
                @Index(name = "idx_builder_improvement_issue_status", columnList = "profile_id, status"),
                @Index(name = "idx_builder_improvement_issue_public_sort", columnList = "profile_id, published, active, deleted, display_order, id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementIssueEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent improvement profile.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private BuilderImprovementProfileEntity profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 60)
    private BuilderImprovementIssueType issueType;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private BuilderImprovementIssueStatus status = BuilderImprovementIssueStatus.PENDING;

    @Column(name = "reported_at")
    private OffsetDateTime reportedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "target_resolution_date")
    private LocalDate targetResolutionDate;

    @Column(name = "resolution_summary", columnDefinition = "text")
    private String resolutionSummary;

    @Column(name = "delay_reason", columnDefinition = "text")
    private String delayReason;

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

    public boolean isResolved() {
        return status != null && status.isResolved();
    }

    public boolean isOpen() {
        return status != null && status.isOpen();
    }
}