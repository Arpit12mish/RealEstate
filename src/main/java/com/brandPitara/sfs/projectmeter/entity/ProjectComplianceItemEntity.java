package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "project_compliance_item",
    indexes = {
        @Index(name = "idx_project_compliance_project_id", columnList = "project_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectComplianceItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_group", nullable = false, length = 40)
    private ProjectComplianceGroup itemGroup;

    @Column(name = "item_key", nullable = false, length = 80)
    private String itemKey;

    @Column(name = "item_label", nullable = false, length = 120)
    private String itemLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectComplianceStatus status;

    @Column(name = "value_text", length = 255)
    private String valueText;

    @Column(name = "document_url", columnDefinition = "text")
    private String documentUrl;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
}