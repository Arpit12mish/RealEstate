package com.brandPitara.sfs.appscreencontent.entity;

import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenMediaType;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "app_screen_content",
        indexes = {
                @Index(name = "idx_app_screen_content_lookup", columnList = "screen_key,placement,enabled,sort_order"),
                @Index(name = "idx_app_screen_content_window", columnList = "start_at,end_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppScreenContentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_key", nullable = false, length = 50)
    private AppScreenKey screenKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AppScreenPlacement placement;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 30)
    private AppScreenMediaType mediaType;

    @Column(name = "media_url", nullable = false, columnDefinition = "text")
    private String mediaUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Builder.Default
    @Column(name = "background_color", nullable = false, length = 20)
    private String backgroundColor = "#000000";

    @Column(name = "aspect_ratio")
    private Double aspectRatio;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "min_app_version", length = 40)
    private String minAppVersion;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
