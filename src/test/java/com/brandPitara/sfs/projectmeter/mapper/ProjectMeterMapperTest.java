package com.brandPitara.sfs.projectmeter.mapper;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMeterMapperTest {

    @Test
    void completedWithinLatestReraTimelineShowsExtensionLabel() {
        var result = ProjectMeterMapper.computeTimeline(
            LocalDate.of(2023, 6, 30),
            LocalDate.of(2024, 6, 30),
            LocalDate.of(2024, 3, 31),
            2
        );

        assertThat(result.status()).isEqualTo("COMPLETED_WITHIN_REVISED_RERA");
        assertThat(result.label()).isEqualTo("Completed after 2 RERA extensions");
        assertThat(result.delayVsOriginalDays()).isPositive();
        assertThat(result.delayVsLatestReraDays()).isZero();
    }

    @Test
    void activeProjectWithFutureLatestReraDateShowsExtendedOnTrack() {
        var result = ProjectMeterMapper.computeTimeline(
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(90),
            null,
            2
        );

        assertThat(result.status()).isEqualTo("EXTENDED_ON_TRACK");
        assertThat(result.label()).isEqualTo("RERA timeline extended 2 times");
    }

    @Test
    void activeProjectPastLatestReraDateShowsDelayed() {
        var result = ProjectMeterMapper.computeTimeline(
            LocalDate.now().minusDays(120),
            LocalDate.now().minusDays(10),
            null,
            1
        );

        assertThat(result.status()).isEqualTo("DELAYED");
        assertThat(result.label()).isEqualTo("10d Delayed");
    }

    @Test
    void cardStartedOnUsesCoreProjectStartDateEvenWhenSnapshotHasConstructionStartDate() {
        ProjectEntity project = new ProjectEntity();
        project.setId(41L);
        project.setName("Project 41");
        project.setStartDate(LocalDate.of(2023, 4, 1));

        ProjectMeterSnapshotEntity snapshot = ProjectMeterSnapshotEntity.builder()
            .constructionStartDate(LocalDate.of(2022, 12, 15))
            .constructionProgressPercent(50)
            .build();

        var response = ProjectMeterMapper.toCardResponse(project, snapshot, List.of());

        assertThat(response.getProjectStartDate()).isEqualTo(LocalDate.of(2023, 4, 1));
        assertThat(response.getStartedOn()).isEqualTo(LocalDate.of(2023, 4, 1));
        assertThat(response.getFavoriteCount()).isZero();
        assertThat(response.getIsFavorite()).isFalse();
    }

    @Test
    void cardStartedOnStaysNullWhenCoreProjectStartDateIsMissing() {
        ProjectEntity project = new ProjectEntity();
        project.setId(41L);
        project.setName("Project 41");

        ProjectMeterSnapshotEntity snapshot = ProjectMeterSnapshotEntity.builder()
            .constructionStartDate(LocalDate.of(2022, 12, 15))
            .build();

        var response = ProjectMeterMapper.toCardResponse(project, snapshot, List.of());

        assertThat(response.getProjectStartDate()).isNull();
        assertThat(response.getStartedOn()).isNull();
    }

    @Test
    void meterMediaKeepsOnlyPublicSafeImagesAndOrdersBySortOrderThenId() {
        var response = ProjectMeterMapper.toMediaResponse(List.of(
            media(30L, ProjectMediaType.IMAGE, "https://cdn/third.jpg", 2, true, false),
            media(12L, ProjectMediaType.IMAGE, "https://cdn/second.jpg", 1, true, false),
            media(11L, ProjectMediaType.IMAGE, "https://cdn/cover.jpg", 1, true, false),
            media(40L, ProjectMediaType.BROCHURE_PDF, "https://cdn/brochure.pdf", 0, true, false),
            media(41L, ProjectMediaType.VIDEO, "https://cdn/video.mp4", 0, true, false),
            media(42L, ProjectMediaType.IMAGE, "https://cdn/inactive.jpg", 0, false, false),
            media(43L, ProjectMediaType.IMAGE, "https://cdn/deleted.jpg", 0, true, true)
        ));

        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn/cover.jpg");
        assertThat(response.getItems())
            .extracting(item -> item.getId())
            .containsExactly(11L, 12L, 30L);
        assertThat(response.getItems())
            .extracting(item -> item.getCover())
            .containsExactly(true, false, false);
    }

    @Test
    void meterMediaReturnsEmptyItemsWhenNoUsableImageExists() {
        var response = ProjectMeterMapper.toMediaResponse(List.of(
            media(1L, ProjectMediaType.BROCHURE_PDF, "https://cdn/brochure.pdf", 0, true, false)
        ));

        assertThat(response.getCoverImageUrl()).isNull();
        assertThat(response.getItems()).isEmpty();
    }

    private ProjectMediaEntity media(Long id, ProjectMediaType type, String url, int sortOrder,
                                     boolean active, boolean deleted) {
        return ProjectMediaEntity.builder()
            .id(id)
            .mediaType(type)
            .url(url)
            .caption("caption-" + id)
            .sortOrder(sortOrder)
            .active(active)
            .deleted(deleted)
            .build();
    }
}
