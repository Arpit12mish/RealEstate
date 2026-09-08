package com.brandPitara.sfs.cms.taxonomy.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cms_content_category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cms_content_category_slug", columnNames = "slug"),
        @UniqueConstraint(name = "uk_cms_content_category_name_ci", columnNames = "normalized_name")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CmsContentCategoryEntity extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "normalized_name", nullable = false, length = 150)
    private String normalizedName;
    @Column(nullable = false, length = 180)
    private String slug;
    @Column(length = 500)
    private String description;
    @Column(nullable = false) @Builder.Default
    private Boolean active = true;
    @Version @Column(nullable = false) @Builder.Default
    private Long version = 0L;
}
