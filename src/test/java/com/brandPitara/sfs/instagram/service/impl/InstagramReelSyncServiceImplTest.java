package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.client.*;
import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
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

    @Test
    void missingConfigReturnsClearError() {
        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            new InstagramMetaProperties(),
            mock(InstagramMetaClient.class),
            mock(InstagramReelRepository.class),
            new InstagramReelMapper()
        );

        assertThatThrownBy(service::syncLatestReels)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Instagram Meta sync is not configured");
    }

    @Test
    void syncCreatesNewReelFromMetaMedia() {
        InstagramMetaClient client = mock(InstagramMetaClient.class);
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

        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            properties,
            client,
            repository,
            new InstagramReelMapper()
        );

        var result = service.syncLatestReels();

        assertThat(result.getFetchedCount()).isEqualTo(1);
        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isZero();
        verify(repository).save(argThat(entity ->
            "media-1".equals(entity.getInstagramMediaId())
                && "https://www.instagram.com/reel/ABC123/".equals(entity.getInstagramUrl())
                && entity.getViewCount() == 1000L
                && entity.getTrendingScore().doubleValue() > 0
        ));
    }

    @Test
    void insightFailureDoesNotFailWholeSync() {
        InstagramMetaClient client = mock(InstagramMetaClient.class);
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramMetaMedia media = reelMedia("media-2");

        when(client.fetchMedia()).thenReturn(List.of(media));
        when(client.fetchInsights("media-2")).thenThrow(new InstagramMetaException("insights failed", null));
        when(repository.findByInstagramMediaIdAndDeletedFalse("media-2")).thenReturn(Optional.empty());
        when(repository.findByDeletedFalse()).thenReturn(List.of());
        when(repository.save(any(InstagramReelEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstagramReelSyncServiceImpl service = new InstagramReelSyncServiceImpl(
            configuredProperties(),
            client,
            repository,
            new InstagramReelMapper()
        );

        var result = service.syncLatestReels();

        assertThat(result.getFailedInsightCount()).isEqualTo(1);
        assertThat(result.getCreatedCount()).isEqualTo(1);
    }

    private InstagramMetaProperties configuredProperties() {
        InstagramMetaProperties properties = new InstagramMetaProperties();
        properties.setGraphApiVersion("v25.0");
        properties.setAccessToken("token");
        properties.setInstagramBusinessAccountId("17841479727273049");
        return properties;
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
