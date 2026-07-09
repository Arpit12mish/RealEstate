package com.brandPitara.sfs.instagram.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramReelSyncResult {
    private Integer fetchedCount;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer skippedCount;
    private Integer failedInsightCount;
    private Integer thumbnailCachedCount;
    private Integer thumbnailCacheFailedCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
