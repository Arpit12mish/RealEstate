package com.brandPitara.sfs.provider.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider_media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderMediaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfileEntity provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private ProviderMediaType mediaType;

    @Column(name = "storage_key", length = 500)
    private String storageKey;


    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
