package com.brandPitara.sfs.cms.author.entity;

import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cms_public_author", uniqueConstraints =
        @UniqueConstraint(name = "uk_cms_public_author_slug", columnNames = "slug"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CmsPublicAuthorEntity extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;
    @Column(nullable = false, length = 180)
    private String slug;
    @Column(length = 2000)
    private String bio;
    @Column(length = 150)
    private String designation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_media_asset_id",
            foreignKey = @ForeignKey(name = "fk_cms_public_author_profile_media"))
    private CmsMediaAssetEntity profileMediaAsset;
    @Column(nullable = false) @Builder.Default
    private Boolean active = true;
    @Version @Column(nullable = false) @Builder.Default
    private Long version = 0L;
}
