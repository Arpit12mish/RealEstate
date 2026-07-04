package com.brandPitara.sfs.builderhighlight.entity;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightMediaType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightSourceType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "builder_highlight_item",
    indexes = {
        @Index(name = "idx_builder_highlight_item_builder", columnList = "builder_id"),
        @Index(name = "idx_builder_highlight_item_type", columnList = "highlight_type"),
        @Index(name = "idx_builder_highlight_item_status", columnList = "status"),
        @Index(name = "idx_builder_highlight_item_public_visible", columnList = "public_visible"),
        @Index(name = "idx_builder_highlight_item_active", columnList = "active"),
        @Index(name = "idx_builder_highlight_item_featured", columnList = "featured"),
        @Index(name = "idx_builder_highlight_item_published_at", columnList = "published_at"),
        @Index(name = "idx_builder_highlight_item_sort_order", columnList = "sort_order"),
        @Index(name = "idx_builder_highlight_item_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "builder_id", nullable = false)
    private BuilderEntity builder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private CityEntity city;

    @Enumerated(EnumType.STRING)
    @Column(name = "highlight_type", nullable = false, length = 40)
    private BuilderHighlightType highlightType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private BuilderHighlightSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 40)
    private BuilderHighlightMediaType mediaType;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 255)
    private String subtitle;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "tag_label", length = 80)
    private String tagLabel;

    @Column(name = "tag_type", length = 60)
    private String tagType;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "video_url", columnDefinition = "text")
    private String videoUrl;

    @Column(name = "youtube_video_id", length = 80)
    private String youtubeVideoId;

    @Column(name = "external_url", columnDefinition = "text")
    private String externalUrl;

    @Builder.Default
    @Column(name = "webview_enabled", nullable = false)
    private Boolean webviewEnabled = false;

    @Column(name = "publisher_name", length = 180)
    private String publisherName;

    @Column(name = "author_label", length = 150)
    private String authorLabel;

    @Builder.Default
    @Column(name = "read_time_minutes", nullable = false)
    private Integer readTimeMinutes = 0;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean featured = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean verified = false;

    @Builder.Default
    @Column(name = "public_visible", nullable = false)
    private Boolean publicVisible = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BuilderHighlightStatus status = BuilderHighlightStatus.DRAFT;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "highlightItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc, id asc")
    @BatchSize(size = 25)
    private List<BuilderHighlightPointEntity> points = new ArrayList<>();

    public boolean isPubliclyVisible() {
        return status == BuilderHighlightStatus.PUBLISHED
            && Boolean.TRUE.equals(publicVisible)
            && Boolean.TRUE.equals(active)
            && deletedAt == null;
    }
}
