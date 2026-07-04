package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
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

    @Test
    void publicHomeItemsCapsAtTwenty() {
        InstagramReelRepository repository = mock(InstagramReelRepository.class);
        InstagramMetaProperties properties = new InstagramMetaProperties();
        properties.setPublicLimit(50);
        InstagramReelServiceImpl service = new InstagramReelServiceImpl(
            repository,
            new InstagramReelMapper(),
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
            new InstagramReelMapper(),
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
            new InstagramReelMapper(),
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
            new InstagramReelMapper(),
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
            new InstagramReelMapper(),
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
            new InstagramReelMapper(),
            new InstagramMetaProperties()
        );
        when(repository.dashboardSearch(isNull(), isNull(), eq("%reel%"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(reel(5L))));

        var page = service.dashboardList(null, null, " Reel ", Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        verify(repository).dashboardSearch(isNull(), isNull(), eq("%reel%"), any(Pageable.class));
        verify(repository, never()).dashboardList(any(), any(), any(Pageable.class));
    }

    private InstagramReelEntity reel(Long id) {
        return InstagramReelEntity.builder()
            .id(id)
            .title("Reel " + id)
            .caption("Caption")
            .instagramUrl("https://www.instagram.com/reel/" + id + "/")
            .thumbnailUrl("https://cdn.example.com/" + id + ".webp")
            .publishedAt(OffsetDateTime.now())
            .active(true)
            .deleted(false)
            .build();
    }
}
