package com.brandPitara.sfs.cms.content.entity;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.workflow.entity.ContentPostRevisionEntity;
import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentTagEntity;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "content_post",
        uniqueConstraints = @UniqueConstraint(name = "uk_content_post_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_content_post_status_updated", columnList = "status, updated_at"),
                @Index(name = "idx_content_post_type_status_updated", columnList = "content_type, status, updated_at"),
                @Index(name = "idx_content_post_owner_status_updated",
                        columnList = "content_owner_dashboard_user_id, status, updated_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ContentPostEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    @ToString.Include
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    @ToString.Include
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, length = 180)
    @ToString.Include
    private String slug;

    @Column(length = 500)
    private String excerpt;

    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_author_id", foreignKey = @ForeignKey(name = "fk_content_post_public_author"))
    private CmsPublicAuthorEntity publicAuthor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_content_post_category"))
    private CmsContentCategoryEntity category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "content_post_tag",
            joinColumns = @JoinColumn(name = "content_post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_content_post_tag", columnNames = {"content_post_id", "tag_id"}))
    @Builder.Default
    private Set<CmsContentTagEntity> tags = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_media_asset_id", foreignKey = @ForeignKey(name = "fk_content_post_cover_media"))
    private CmsMediaAssetEntity coverMediaAsset;

    @Column(name = "cover_alt_text", length = 300)
    private String coverAltText;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "content_owner_dashboard_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_post_owner_dashboard_user")
    )
    private DashboardUserEntity contentOwner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_dashboard_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_post_created_by_dashboard_user")
    )
    private DashboardUserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "updated_by_dashboard_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_post_updated_by_dashboard_user")
    )
    private DashboardUserEntity updatedBy;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "canonical_url", length = 2048)
    private String canonicalUrl;

    @Column(name = "robots_index", nullable = false)
    @Builder.Default
    private Boolean robotsIndex = true;

    @Column(name = "robots_follow", nullable = false)
    @Builder.Default
    private Boolean robotsFollow = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_document", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private ContentDocument contentDocument = ContentDocument.empty();

    @Column(name = "content_document_schema_version", nullable = false)
    @Builder.Default
    private Short contentDocumentSchemaVersion = (short) ContentDocument.CURRENT_SCHEMA_VERSION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_review_revision_id",
            foreignKey = @ForeignKey(name = "fk_content_post_current_review_revision"))
    private ContentPostRevisionEntity currentReviewRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_revision_id",
            foreignKey = @ForeignKey(name = "fk_content_post_approved_revision"))
    private ContentPostRevisionEntity approvedRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_published_revision_id",
            foreignKey = @ForeignKey(name = "fk_content_post_current_published_revision"))
    private ContentPostRevisionEntity currentPublishedRevision;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_dashboard_user_id",
            foreignKey = @ForeignKey(name = "fk_content_post_published_by_dashboard_user"))
    private DashboardUserEntity publishedBy;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
