package com.brandPitara.sfs.builderhighlight.entity;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightPointType;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "builder_highlight_point",
    indexes = {
        @Index(name = "idx_builder_highlight_point_item_sort", columnList = "highlight_item_id, active, display_order, id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightPointEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "highlight_item_id", nullable = false)
    private BuilderHighlightItemEntity highlightItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false, length = 40)
    private BuilderHighlightPointType pointType;

    @Column(length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String text;

    @Column(name = "icon_key", length = 80)
    private String iconKey;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
