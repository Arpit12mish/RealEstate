package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The durable idempotency/provenance record for one WordPress source post - never the post body
 * itself, only enough to detect a rerun (same fingerprint -> skip) versus a changed source (fail
 * safe, see {@code source_fingerprint}). {@code targetContentId} is null for a blocked source
 * that never produced a content_post row.
 */
@Entity
@Table(name = "wordpress_migration_mapping", uniqueConstraints = @UniqueConstraint(
        name = "uk_wordpress_migration_mapping_source", columnNames = {"source_system", "source_post_id"}
))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordPressMigrationMappingEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_system", nullable = false, length = 40)
    private String sourceSystem;

    @Column(name = "source_post_id", nullable = false)
    private Long sourcePostId;

    @Column(name = "target_content_id")
    private Long targetContentId;

    @Column(name = "source_fingerprint", nullable = false, length = 64)
    private String sourceFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "migration_state", nullable = false, length = 30)
    private WordPressMigrationState migrationState;

    @Column(name = "error_code", length = 60)
    private String errorCode;
}
