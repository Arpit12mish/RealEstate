package com.brandPitara.sfs.cms.taxonomy.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cms_content_tag", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cms_content_tag_slug", columnNames = "slug"),
        @UniqueConstraint(name = "uk_cms_content_tag_name_ci", columnNames = "normalized_name")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CmsContentTagEntity extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;
    @Column(nullable = false, length = 180)
    private String slug;
    @Column(nullable = false) @Builder.Default
    private Boolean active = true;
    @Version @Column(nullable = false) @Builder.Default
    private Long version = 0L;
}
