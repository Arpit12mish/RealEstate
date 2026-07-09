package com.brandPitara.sfs.instagram.dto;

import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardInstagramReelResponse {
    private Long id;
    private String instagramMediaId;
    private String caption;
    private String title;
    private String instagramUrl;
    private String thumbnailUrl;
    private String sourceThumbnailUrl;
    private String cachedThumbnailUrl;
    private String cachedThumbnailStorageKey;
    private OffsetDateTime thumbnailCachedAt;
    private OffsetDateTime metaFetchedAt;
    private String lastSyncStatus;
    private String lastSyncError;
    private String previewVideoUrl;
    private String mediaType;
    private String mediaProductType;
    private OffsetDateTime publishedAt;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Long saveCount;
    private BigDecimal trendingScore;
    private InstagramReelCategory categoryOverride;
    private Integer displayOrder;
    private Boolean active;
    private Boolean deleted;
    private Boolean syncedFromMeta;
    private OffsetDateTime lastSyncedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
