package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectConstructionStageCode;
import com.brandPitara.sfs.projectmeter.enums.ProjectStageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "project_construction_stage",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_stage_code", columnNames = {"project_id", "stage_code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConstructionStageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_code", nullable = false, length = 50)
    private ProjectConstructionStageCode stageCode;

    @Column(name = "stage_label", nullable = false, length = 120)
    private String stageLabel;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "weight_percent", nullable = false)
    private Integer weightPercent;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProjectStageStatus status = ProjectStageStatus.NOT_STARTED;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "evidence_count", nullable = false)
    @Builder.Default
    private Integer evidenceCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
}