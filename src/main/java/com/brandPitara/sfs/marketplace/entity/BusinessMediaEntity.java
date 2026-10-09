package com.brandPitara.sfs.marketplace.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessMediaUsage;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessMediaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 20)
    private BusinessMediaUsage usageType;

    @Column(name = "media_url", nullable = false, columnDefinition = "text")
    private String mediaUrl;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
