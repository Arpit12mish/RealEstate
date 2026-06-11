package com.brandPitara.sfs.builderimprovement.entity;

import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementEvidenceStatus;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementTimelineType;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "builder_improvement_timeline",
        indexes = {
                @Index(name = "idx_builder_improvement_timeline_profile", columnList = "profile_id"),
                @Index(name = "idx_builder_improvement_timeline_public_sort", columnList = "profile_id, published, active, deleted, display_order, id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementTimelineEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent improvement profile.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private BuilderImprovementProfileEntity profile;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "timeline_type", nullable = false, length = 60)
    private BuilderImprovementTimelineType timelineType = BuilderImprovementTimelineType.OTHER;

    @Column(name = "icon_key", length = 80)
    private String iconKey;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_month_label", length = 40)
    private String eventMonthLabel;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "short_description", columnDefinition = "text")
    private String shortDescription;

    @Column(name = "problem_observed", columnDefinition = "text")
    private String problemObserved;

    @Column(name = "action_taken", columnDefinition = "text")
    private String actionTaken;

    @Column(name = "current_status", columnDefinition = "text")
    private String currentStatus;

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