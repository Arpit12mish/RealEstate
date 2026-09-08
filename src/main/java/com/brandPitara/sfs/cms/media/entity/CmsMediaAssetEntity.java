package com.brandPitara.sfs.cms.media.entity;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cms_media_asset", indexes = {
        @Index(name = "idx_cms_media_status_created", columnList = "status, created_at"),
        @Index(name = "idx_cms_media_type_status_created", columnList = "media_type, status, created_at"),
        @Index(name = "idx_cms_media_creator_created", columnList = "created_by_dashboard_user_id, created_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_cms_media_storage_key", columnNames = "storage_key"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CmsMediaAssetEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private CmsMediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CmsMediaStatus status;

    @Column(name = "storage_bucket", nullable = false, length = 255)
    private String storageBucket;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "declared_size_bytes", nullable = false)
    private Long declaredSizeBytes;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    private Integer width;
    private Integer height;

    @Column(name = "duration_millis")
    private Long durationMillis;

    @Column(name = "object_etag", length = 128)
    private String objectETag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_dashboard_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cms_media_creator_dashboard_user"))
    private DashboardUserEntity createdBy;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
