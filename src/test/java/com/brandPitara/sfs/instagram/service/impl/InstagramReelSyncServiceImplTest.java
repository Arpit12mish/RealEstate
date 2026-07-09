package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.client.*;
import com.brandPitara.sfs.instagram.config.AppInstagramProperties;
import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.dto.InstagramCachedAsset;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
import com.brandPitara.sfs.instagram.service.InstagramAssetCacheService;
import com.brandPitara.sfs.instagram.service.InstagramReelMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InstagramReelSyncServiceImplTest {

    private static final String FALLBACK_THUMBNAIL_URL =
        "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/banner/Hero/M3M-Jacob-%26-Co-1.webp";

    @Test
    void missingConfigReturnsClearError() {
        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            new InstagramMetaProperties(),
            appProperties(),
            mock(InstagramMetaClient.class),
            mock(InstagramAssetCacheService.class),
            mock(InstagramReelRepository.class),
            mapper()
        );

        assertThatThrownBy(service::syncLatestReels)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Instagram Meta sync is not configured");
    }

    @Test
    void syncCreatesNewReelFromMetaMedia() {
        InstagramMetaClient client = mock(InstagramMetaClient.class);
        InstagramAssetCacheService assetCacheService = mock(InstagramAssetCacheService.class);
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramMetaProperties properties = configuredProperties();
        InstagramMetaMedia media = reelMedia("media-1");

        when(client.fetchMedia()).thenReturn(List.of(media));
        when(client.fetchInsights("media-1")).thenReturn(InstagramMetaInsights.builder()
            .viewCount(1000L)
            .likeCount(50L)
            .commentCount(5L)
            .shareCount(10L)
            .saveCount(7L)
            .build());
        when(repository.findByInstagramMediaIdAndDeletedFalse("media-1")).thenReturn(Optional.empty());
        when(repository.findByDeletedFalse()).thenReturn(List.of());
        when(repository.save(any(InstagramReelEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetCacheService.cacheThumbnail("media-1", "https://cdn.example.com/thumb.jpg"))
            .thenReturn(new InstagramCachedAsset(
                "https://cdn.squarefootstory.com/instagram/reels/media-1/thumb.jpg",
                "instagram/reels/media-1/thumb.jpg"
            ));

        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            properties,
            appProperties(),
            client,
            assetCacheService,
            repository,
            mapper()
        );

        var result = service.syncLatestReels();

        assertThat(result.getFetchedCount()).isEqualTo(1);
        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isZero();
        assertThat(result.getThumbnailCachedCount()).isEqualTo(1);
        verify(repository).save(argThat(entity ->
            "media-1".equals(entity.getInstagramMediaId())
                && "https://www.instagram.com/reel/ABC123/".equals(entity.getInstagramUrl())
                && "https://cdn.example.com/thumb.jpg".equals(entity.getSourceThumbnailUrl())
                && "https://cdn.squarefootstory.com/instagram/reels/media-1/thumb.jpg".equals(entity.getCachedThumbnailUrl())
                && "https://cdn.squarefootstory.com/instagram/reels/media-1/thumb.jpg".equals(entity.getThumbnailUrl())
                && entity.getViewCount() == 1000L
                && entity.getTrendingScore().doubleValue() > 0
        ));
    }

    @Test
    void insightFailureDoesNotFailWholeSync() {
        InstagramMetaClient client = mock(InstagramMetaClient.class);
        InstagramAssetCacheService assetCacheService = mock(InstagramAssetCacheService.class);
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramMetaMedia media = reelMedia("media-2");

        when(client.fetchMedia()).thenReturn(List.of(media));
        when(client.fetchInsights("media-2")).thenThrow(new InstagramMetaException("insights failed", null));
        when(repository.findByInstagramMediaIdAndDeletedFalse("media-2")).thenReturn(Optional.empty());
        when(repository.findByDeletedFalse()).thenReturn(List.of());
        when(repository.save(any(InstagramReelEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetCacheService.cacheThumbnail("media-2", "https://cdn.example.com/thumb.jpg"))
            .thenReturn(new InstagramCachedAsset(
                "https://cdn.squarefootstory.com/instagram/reels/media-2/thumb.jpg",
                "instagram/reels/media-2/thumb.jpg"
            ));

        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            configuredProperties(),
            appProperties(),
            client,
            assetCacheService,
            repository,
            mapper()
        );

        var result = service.syncLatestReels();

        assertThat(result.getFailedInsightCount()).isEqualTo(1);
        assertThat(result.getCreatedCount()).isEqualTo(1);
    }

    @Test
    void syncUpdatesExistingReelByInstagramMediaIdWithoutDuplicating() {
        InstagramMetaClient client = mock(InstagramMetaClient.class);
        InstagramAssetCacheService assetCacheService = mock(InstagramAssetCacheService.class);
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelEntity existing = InstagramReelEntity.builder()
            .id(42L)
            .instagramMediaId("media-3")
            .instagramUrl("https://www.instagram.com/reel/OLD/")
            .sourceThumbnailUrl("https://cdn.example.com/thumb.jpg")
            .cachedThumbnailUrl("https://cdn.squarefootstory.com/instagram/reels/media-3/thumb.jpg")
            .cachedThumbnailStorageKey("instagram/reels/media-3/thumb.jpg")
            .thumbnailCachedAt(OffsetDateTime.now())
            .active(true)
            .deleted(false)
            .build();

        when(client.fetchMedia()).thenReturn(List.of(reelMedia("media-3")));
        when(client.fetchInsights("media-3")).thenReturn(InstagramMetaInsights.builder().build());
        when(repository.findByInstagramMediaIdAndDeletedFalse("media-3")).thenReturn(Optional.of(existing));
        when(repository.findByDeletedFalse()).thenReturn(List.of(existing));
        when(repository.save(any(InstagramReelEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            configuredProperties(),
            appProperties(),
            client,
            assetCacheService,
            repository,
            mapper()
        );

        var result = service.syncLatestReels();

        assertThat(result.getCreatedCount()).isZero();
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        verify(repository).findByInstagramMediaIdAndDeletedFalse("media-3");
        verify(repository).save(same(existing));
    }

    @Test
    void thumbnailCacheFailureKeepsExistingCachedThumbnail() {
        InstagramMetaClient client = mock(InstagramMetaClient.class);
        InstagramAssetCacheService assetCacheService = mock(InstagramAssetCacheService.class);
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelEntity existing = InstagramReelEntity.builder()
            .id(44L)
            .instagramMediaId("media-4")
            .instagramUrl("https://www.instagram.com/reel/ABC123/")
            .sourceThumbnailUrl("https://cdn.example.com/old-thumb.jpg")
            .cachedThumbnailUrl("https://cdn.squarefootstory.com/instagram/reels/media-4/thumb.jpg")
            .cachedThumbnailStorageKey("instagram/reels/media-4/thumb.jpg")
            .thumbnailCachedAt(OffsetDateTime.now().minusDays(3))
            .thumbnailUrl("https://cdn.squarefootstory.com/instagram/reels/media-4/thumb.jpg")
            .active(true)
            .deleted(false)
            .build();

        when(client.fetchMedia()).thenReturn(List.of(reelMedia("media-4")));
        when(client.fetchInsights("media-4")).thenReturn(InstagramMetaInsights.builder().build());
        when(repository.findByInstagramMediaIdAndDeletedFalse("media-4")).thenReturn(Optional.of(existing));
        when(repository.findByDeletedFalse()).thenReturn(List.of(existing));
        when(repository.save(any(InstagramReelEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetCacheService.cacheThumbnail("media-4", "https://cdn.example.com/thumb.jpg"))
            .thenThrow(new IllegalStateException("download failed"));

        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            configuredProperties(),
            appProperties(),
            client,
            assetCacheService,
            repository,
            mapper()
        );

        var result = service.syncLatestReels();

        assertThat(result.getThumbnailCacheFailedCount()).isEqualTo(1);
        assertThat(existing.getCachedThumbnailUrl())
            .isEqualTo("https://cdn.squarefootstory.com/instagram/reels/media-4/thumb.jpg");
        assertThat(existing.getLastSyncStatus()).isEqualTo("THUMBNAIL_CACHE_FAILED");
    }

    @Test
    void recacheMissingThumbnailsUsesLegacyThumbnailOnlyAsInternalSource() {
        InstagramMetaClient client = mock(InstagramMetaClient.class);
        InstagramAssetCacheService assetCacheService = mock(InstagramAssetCacheService.class);
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelEntity missing = InstagramReelEntity.builder()
            .id(55L)
            .instagramMediaId("media-5")
            .instagramUrl("https://www.instagram.com/reel/ABC123/")
            .thumbnailUrl("https://lookaside.instagram.com/asset/?X-Amz-Signature=expired")
            .active(true)
            .deleted(false)
            .build();

        when(repository.findByActiveTrueAndDeletedFalseAndCachedThumbnailUrlIsNull())
            .thenReturn(List.of(missing));
        when(repository.save(any(InstagramReelEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetCacheService.cacheThumbnail("media-5", "https://lookaside.instagram.com/asset/?X-Amz-Signature=expired"))
            .thenReturn(new InstagramCachedAsset(
                "https://cdn.squarefootstory.com/instagram/reels/media-5/thumb.jpg",
                "instagram/reels/media-5/thumb.jpg"
            ));

        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            configuredProperties(),
            appProperties(),
            client,
            assetCacheService,
            repository,
            mapper()
        );

        var result = service.recacheMissingThumbnails();

        assertThat(result.getThumbnailCachedCount()).isEqualTo(1);
        assertThat(missing.getCachedThumbnailUrl())
            .isEqualTo("https://cdn.squarefootstory.com/instagram/reels/media-5/thumb.jpg");
        assertThat(missing.getThumbnailUrl())
            .isEqualTo("https://cdn.squarefootstory.com/instagram/reels/media-5/thumb.jpg");
    }

    private InstagramMetaProperties configuredProperties() {
        InstagramMetaProperties properties = new InstagramMetaProperties();
        properties.setGraphApiVersion("v25.0");
        properties.setAccessToken("token");
        properties.setInstagramBusinessAccountId("17841479727273049");
        return properties;
    }

    private AppInstagramProperties appProperties() {
        AppInstagramProperties properties = new AppInstagramProperties();
        properties.setThumbnailCacheTtlHours(24);
        properties.setFallbackThumbnailUrl(FALLBACK_THUMBNAIL_URL);
        return properties;
    }

    private InstagramReelMapper mapper() {
        return new InstagramReelMapper(appProperties());
    }

    private InstagramMetaMedia reelMedia(String id) {
        return new InstagramMetaMedia(
            id,
            "First line title\nMore caption",
            "VIDEO",
            "REELS",
            "https://www.instagram.com/reel/ABC123/",
            "https://cdn.example.com/thumb.jpg",
            OffsetDateTime.now().minusDays(2),
            40L,
            3L
        );
    }
}
