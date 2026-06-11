package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "project_amenity_progress",
    indexes = {
        @Index(name = "idx_project_amenity_progress_project_id", columnList = "project_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAmenityProgressEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "amenity_code", nullable = false, length = 80)
    private String amenityCode;

    @Column(name = "amenity_label", nullable = false, length = 120)
    private String amenityLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectAmenityStatus status;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "weight_percent", nullable = false)
    @Builder.Default
    private Integer weightPercent = 0;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    // --- Amenity Intelligence v1 ---

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private ProjectAmenityCategory category;

    @Column(name = "category_label", length = 100)
    private String categoryLabel;

    @Column(name = "icon_key", length = 80)
    private String iconKey;

    @Column(name = "rare", nullable = false)
    @Builder.Default
    private Boolean rare = false;

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "public_visible", nullable = false)
    @Builder.Default
    private Boolean publicVisible = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "category_display_order", nullable = false)
    @Builder.Default
    private Integer categoryDisplayOrder = 0;
}
