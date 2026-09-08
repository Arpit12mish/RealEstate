package com.brandPitara.sfs.cms.workflow.entity;

import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason;
import com.brandPitara.sfs.cms.workflow.domain.ContentTagSnapshot;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Immutable
@Table(name = "content_post_revision", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_revision_post_number", columnNames = {
                "content_post_id", "revision_number"
        }),
        @UniqueConstraint(name = "uk_content_revision_id_post", columnNames = {
                "id", "content_post_id"
        })
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentPostRevisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_revision_post"))
    private ContentPostEntity contentPost;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    @Column(name = "public_author_id")
    private Long publicAuthorId;
    @Column(name = "public_author_name", length = 150)
    private String publicAuthorName;
    @Column(name = "public_author_slug", length = 180)
    private String publicAuthorSlug;
    @Column(name = "public_author_designation", length = 150)
    private String publicAuthorDesignation;
    @Column(name = "public_author_profile_media_asset_id")
    private Long publicAuthorProfileMediaAssetId;

    @Column(name = "category_id")
    private Long categoryId;
    @Column(name = "category_name", length = 150)
    private String categoryName;
    @Column(name = "category_slug", length = 180)
    private String categorySlug;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tag_snapshots", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<ContentTagSnapshot> tagSnapshots = List.of();

    @Column(name = "cover_media_asset_id")
    private Long coverMediaAssetId;
    @Column(name = "cover_alt_text", length = 300)
    private String coverAltText;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(name = "robots_index", nullable = false)
    private Boolean robotsIndex;

    @Column(name = "robots_follow", nullable = false)
    private Boolean robotsFollow;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_document", columnDefinition = "jsonb", nullable = false)
    private ContentDocument contentDocument;

    @Column(name = "content_document_schema_version", nullable = false)
    private Short contentDocumentSchemaVersion;

    @Column(name = "created_from_post_version", nullable = false)
    private Long createdFromPostVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_dashboard_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_revision_creator"))
    private DashboardUserEntity createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "revision_reason", nullable = false, length = 32)
    private ContentRevisionReason revisionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
