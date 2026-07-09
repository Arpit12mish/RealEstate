package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.config.AppInstagramProperties;
import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelUpsertRequest;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
import com.brandPitara.sfs.instagram.service.InstagramReelMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class InstagramReelServiceImplTest {

    private static final String FALLBACK_THUMBNAIL_URL =
        "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/banner/Hero/M3M-Jacob-%26-Co-1.webp";

    @Test
    void publicHomeItemsCapsAtTwenty() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramMetaProperties properties = new InstagramMetaProperties();
        properties.setPublicLimit(50);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            properties
        );
        InstagramReelEntity reel = reel(1L);
        when(repository.findByActiveTrueAndDeletedFalseOrderByPublishedAtDescIdDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel)));

        var items = service.publicHomeItems(99);

        assertThat(items).hasSize(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByActiveTrueAndDeletedFalseOrderByPublishedAtDescIdDesc(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void publicListUsesInterviewOverrideQuery() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        when(repository.findByActiveTrueAndDeletedFalseAndCategoryOverrideOrderByDisplayOrderAscPublishedAtDescIdDesc(
            eq(InstagramReelCategory.INTERVIEWS),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(reel(2L))));

        var items = service.publicList(InstagramReelCategory.INTERVIEWS, 0, 10);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getCategory()).isEqualTo(InstagramReelCategory.INTERVIEWS);
    }

    @Test
    void manualCreateRequiresThumbnailUrl() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );

        DashboardInstagramReelUpsertRequest request = DashboardInstagramReelUpsertRequest.builder()
            .instagramUrl("https://www.instagram.com/reel/ABC123/")
            .build();

        assertThatThrownBy(() -> service.createManual(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("thumbnailUrl is required");
    }

    @Test
    void dashboardListWithNullQueryUsesNoSearchRepositoryMethod() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        when(repository.dashboardList(isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel(3L))));

        var page = service.dashboardList(null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        verify(repository).dashboardList(isNull(), isNull(), any(Pageable.class));
        verify(repository, never()).dashboardSearch(any(), any(), anyString(), any(Pageable.class));
    }

    @Test
    void dashboardListWithBlankQueryUsesNoSearchRepositoryMethod() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        when(repository.dashboardList(eq(true), eq(InstagramReelCategory.MANUAL), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel(4L))));

        var page = service.dashboardList(true, InstagramReelCategory.MANUAL, "   ", Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        verify(repository).dashboardList(eq(true), eq(InstagramReelCategory.MANUAL), any(Pageable.class));
        verify(repository, never()).dashboardSearch(any(), any(), anyString(), any(Pageable.class));
    }

    @Test
    void dashboardListWithSearchQueryUsesSearchRepositoryMethod() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        when(repository.dashboardSearch(isNull(), isNull(), eq("%reel%"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel(5L))));

        var page = service.dashboardList(null, null, " Reel ", Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        verify(repository).dashboardSearch(isNull(), isNull(), eq("%reel%"), any(Pageable.class));
        verify(repository, never()).dashboardList(any(), any(), any(Pageable.class));
    }

    @Test
    void dashboardResponseReturnsCachedThumbnailUrlWhenPresent() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        InstagramReelEntity reel = reel(8L);
        reel.setThumbnailUrl("https://scontent.cdninstagram.com/temp.jpg?X-Amz-Signature=expired");
        reel.setCachedThumbnailUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/instagram/reels/media-8/thumb.jpg");
        when(repository.dashboardList(isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel)));

        var page = service.dashboardList(null, null, null, Pageable.unpaged());

        assertThat(page.getContent().get(0).getThumbnailUrl())
            .isEqualTo("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/instagram/reels/media-8/thumb.jpg");
        assertThat(page.getContent().get(0).getCachedThumbnailUrl())
            .isEqualTo("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/instagram/reels/media-8/thumb.jpg");
    }

    @Test
    void dashboardResponseFallsBackWhenCachedThumbnailIsMissing() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        InstagramReelEntity reel = reel(9L);
        reel.setCachedThumbnailUrl(null);
        reel.setThumbnailUrl("https://scontent.cdninstagram.com/temp.jpg?oe=expired&_nc_sid=1");
        when(repository.dashboardList(isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel)));

        var page = service.dashboardList(null, null, null, Pageable.unpaged());

        assertThat(page.getContent().get(0).getThumbnailUrl()).isEqualTo(FALLBACK_THUMBNAIL_URL);
        assertThat(page.getContent().get(0).getCachedThumbnailUrl()).isNull();
    }

    @Test
    void dashboardResponseDoesNotExposeUnsafeCachedThumbnailAsRenderableUrl() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        InstagramReelEntity reel = reel(10L);
        reel.setCachedThumbnailUrl("https://lookaside.instagram.com/asset/?X-Amz-Signature=expired");
        reel.setThumbnailUrl("https://scontent.cdninstagram.com/temp.jpg?_nc_ht=scontent");
        when(repository.dashboardList(isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel)));

        var page = service.dashboardList(null, null, null, Pageable.unpaged());

        String thumbnailUrl = page.getContent().get(0).getThumbnailUrl();
        assertThat(thumbnailUrl).isEqualTo(FALLBACK_THUMBNAIL_URL);
        assertThat(page.getContent().get(0).getCachedThumbnailUrl()).isNull();
        assertThat(thumbnailUrl)
            .doesNotContain("cdninstagram")
            .doesNotContain("scontent")
            .doesNotContain("lookaside")
            .doesNotContain("X-Amz-Signature");
    }

    @Test
    void publicListReturnsCachedThumbnailUrlWhenPresent() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        InstagramReelEntity reel = reel(6L);
        reel.setThumbnailUrl("https://scontent.cdninstagram.com/temp.jpg?X-Amz-Signature=expired");
        reel.setCachedThumbnailUrl("https://cdn.squarefootstory.com/instagram/reels/media-6/thumb.jpg");
        when(repository.findByActiveTrueAndDeletedFalseOrderByPublishedAtDescIdDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel)));

        var items = service.publicList(InstagramReelCategory.LATEST, 0, 10);

        assertThat(items.get(0).getThumbnailUrl())
            .isEqualTo("https://cdn.squarefootstory.com/instagram/reels/media-6/thumb.jpg");
    }

    @Test
    void publicListFallsBackAndDoesNotExposeMetaThumbnailUrl() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            mapper(),
            new InstagramMetaProperties()
        );
        InstagramReelEntity reel = reel(7L);
        reel.setCachedThumbnailUrl(null);
        reel.setThumbnailUrl("https://lookaside.instagram.com/asset/?X-Amz-Signature=expired&scontent=1");
        when(repository.findByActiveTrueAndDeletedFalseOrderByPublishedAtDescIdDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel)));

        var items = service.publicList(InstagramReelCategory.LATEST, 0, 10);

        String thumbnailUrl = items.get(0).getThumbnailUrl();
        assertThat(thumbnailUrl).isEqualTo(FALLBACK_THUMBNAIL_URL);
        assertThat(thumbnailUrl)
            .doesNotContain("scontent")
            .doesNotContain("lookaside")
            .doesNotContain("X-Amz-Signature");
    }

    private InstagramReelEntity reel(Long id) {
        return InstagramReelEntity.builder()
            .id(id)
            .instagramMediaId("media-" + id)
            .title("Reel " + id)
            .caption("Caption")
            .instagramUrl("https://www.instagram.com/reel/" + id + "/")
            .thumbnailUrl("https://cdn.example.com/" + id + ".webp")
            .cachedThumbnailUrl("https://cdn.example.com/" + id + ".webp")
            .publishedAt(OffsetDateTime.now())
            .active(true)
            .deleted(false)
            .build();
    }

    private InstagramReelMapper mapper() {
        AppInstagramProperties properties = new AppInstagramProperties();
        properties.setFallbackThumbnailUrl(FALLBACK_THUMBNAIL_URL);
        return new InstagramReelMapper(properties);
    }
}
