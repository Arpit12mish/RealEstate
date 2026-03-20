package com.brandPitara.sfs.projectmeter.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "project_price_history",
    indexes = {
        @Index(name = "idx_project_price_history_project_id", columnList = "project_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPriceHistoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "year_label", nullable = false, length = 20)
    private String yearLabel;

    @Column(name = "project_price", nullable = false)
    private Long projectPrice;

    @Column(name = "average_area_price")
    private Long averageAreaPrice;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
}