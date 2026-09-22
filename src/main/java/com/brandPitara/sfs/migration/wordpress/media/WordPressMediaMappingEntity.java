package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The durable idempotency record for one WordPress attachment - never the image/video bytes
 * themselves, only enough to detect a rerun (same {@code sourceSha256} -> reuse
 * {@code cmsMediaAsset} without re-uploading) versus a changed source (fail safe). Distinct from
 * {@link com.brandPitara.sfs.migration.wordpress.importer.WordPressMigrationMappingEntity}, which
 * tracks WordPress *posts*, not attachments - many attachment rows may point at the same
 * {@code cmsMediaAsset} (byte-identical content reused across attachments), which is intentional:
 * alt text/captions live on the content-document block, not on the media asset.
 */
@Entity
@Table(name = "wordpress_migration_media_mapping", uniqueConstraints = @UniqueConstraint(
        name = "uk_wordpress_migration_media_mapping_source",
        columnNames = {"source_system", "wordpress_attachment_id"}
))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordPressMediaMappingEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_system", nullable = false, length = 40)
    private String sourceSystem;

    @Column(name = "wordpress_attachment_id", nullable = false)
    private Long wordPressAttachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cms_media_asset_id",
            foreignKey = @ForeignKey(name = "fk_wordpress_media_mapping_cms_media_asset"))
    private CmsMediaAssetEntity cmsMediaAsset;

    @Column(name = "source_upload_path", nullable = false, length = 1024)
    private String sourceUploadPath;

    @Column(name = "source_sha256", nullable = false, length = 64)
    private String sourceSha256;

    @Column(name = "source_size_bytes", nullable = false)
    private Long sourceSizeBytes;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "migration_state", nullable = false, length = 30)
    private WordPressMediaMigrationState migrationState;

    @Column(name = "error_code", length = 60)
    private String errorCode;
}
