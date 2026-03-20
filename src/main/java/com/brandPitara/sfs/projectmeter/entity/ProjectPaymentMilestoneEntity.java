package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectConstructionStageCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "project_payment_milestone",
    indexes = {
        @Index(name = "idx_project_payment_milestone_project_id", columnList = "project_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPaymentMilestoneEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "milestone_code", nullable = false, length = 80)
    private String milestoneCode;

    @Column(name = "milestone_label", nullable = false, length = 120)
    private String milestoneLabel;

    @Column(length = 255)
    private String description;

    @Column(name = "percentage_value", nullable = false)
    private Integer percentageValue;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "linked_stage_code", length = 50)
    private ProjectConstructionStageCode linkedStageCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}