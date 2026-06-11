package com.brandPitara.sfs.builderimprovement.entity;

import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementActionType;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementEvidenceStatus;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementImpactLevel;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "builder_improvement_action",
        indexes = {
                @Index(name = "idx_builder_improvement_action_profile", columnList = "profile_id"),
                @Index(name = "idx_builder_improvement_action_public_sort", columnList = "profile_id, published, active, deleted, display_order, id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementActionEntity extends BaseEntity {

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
    @Column(name = "action_type", nullable = false, length = 60)
    private BuilderImprovementActionType actionType;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "context_text", columnDefinition = "text")
    private String contextText;

    @Column(name = "action_text", columnDefinition = "text")
    private String actionText;

    @Column(name = "result_text", columnDefinition = "text")
    private String resultText;

    @Column(name = "icon_key", length = 80)
    private String iconKey;

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