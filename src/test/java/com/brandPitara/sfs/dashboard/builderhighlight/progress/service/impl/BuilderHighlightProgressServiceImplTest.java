package com.brandPitara.sfs.dashboard.builderhighlight.progress.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightBuilderProgressResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightOverviewResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightProgressRow;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightSectionProgressResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightOverallProgressStatus;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightSectionStatus;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.repository.BuilderHighlightProgressRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuilderHighlightProgressServiceImplTest {

    private final BuilderRepository builderRepository = mock(BuilderRepository.class);
    private final BuilderHighlightProgressRepository progressRepository = mock(BuilderHighlightProgressRepository.class);
    private final BuilderHighlightProgressServiceImpl service =
        new BuilderHighlightProgressServiceImpl(builderRepository, progressRepository);

    @Test
    void builderWithNoHighlightsIsNotStartedAndAllSectionsMissing() {
        when(builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(builder(7L, "M3M")));
        when(progressRepository.findProgressRowsByBuilderIds(List.of(7L))).thenReturn(List.of());

        BuilderHighlightBuilderProgressResponse response = service.getBuilderProgress(7L);

        assertThat(response.getOverallStatus()).isEqualTo(BuilderHighlightOverallProgressStatus.NOT_STARTED);
        assertThat(response.getCompletionPercent()).isEqualTo(0);
        assertThat(response.isHighlightsAvailableForMobile()).isFalse();
        assertThat(response.getSections())
            .allSatisfy(s -> assertThat(s.getStatus()).isEqualTo(BuilderHighlightSectionStatus.MISSING));
    }

    @Test
    void builderWithOneDraftBuilderUpdateIsInProgressAndNotMobileReady() {
        when(builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(builder(7L, "M3M")));
        when(progressRepository.findProgressRowsByBuilderIds(List.of(7L))).thenReturn(List.of(
            row(7L, "M3M", BuilderHighlightType.BUILDER_UPDATE, BuilderHighlightStatus.DRAFT, false, true)
        ));

        BuilderHighlightBuilderProgressResponse response = service.getBuilderProgress(7L);

        assertThat(response.getOverallStatus()).isEqualTo(BuilderHighlightOverallProgressStatus.IN_PROGRESS);
        assertThat(response.isHighlightsAvailableForMobile()).isFalse();
        BuilderHighlightSectionProgressResponse builderUpdate = sectionOf(response, BuilderHighlightType.BUILDER_UPDATE);
        assertThat(builderUpdate.getStatus()).isEqualTo(BuilderHighlightSectionStatus.DRAFT);
    }

    @Test
    void builderWithAllFourSectionsPublishedIsReadyAndMobileReady() {
        when(builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(builder(7L, "M3M")));
        when(progressRepository.findProgressRowsByBuilderIds(List.of(7L))).thenReturn(List.of(
            row(7L, "M3M", BuilderHighlightType.BUILDER_UPDATE, BuilderHighlightStatus.PUBLISHED, true, true),
            row(7L, "M3M", BuilderHighlightType.SOCIAL_IMPACT, BuilderHighlightStatus.PUBLISHED, true, true),
            row(7L, "M3M", BuilderHighlightType.NEWS_ARTICLE, BuilderHighlightStatus.PUBLISHED, true, true),
            row(7L, "M3M", BuilderHighlightType.SFS_ANALYSIS, BuilderHighlightStatus.PUBLISHED, true, true)
        ));

        BuilderHighlightBuilderProgressResponse response = service.getBuilderProgress(7L);

        assertThat(response.getOverallStatus()).isEqualTo(BuilderHighlightOverallProgressStatus.READY);
        assertThat(response.getCompletionPercent()).isEqualTo(100);
        assertThat(response.isHighlightsAvailableForMobile()).isTrue();
    }

    @Test
    void sectionWithPublicVisibleFalseIsNotCompleteAndNotMobileReady() {
        when(builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(builder(7L, "M3M")));
        when(progressRepository.findProgressRowsByBuilderIds(List.of(7L))).thenReturn(List.of(
            row(7L, "M3M", BuilderHighlightType.BUILDER_UPDATE, BuilderHighlightStatus.PUBLISHED, false, true),
            row(7L, "M3M", BuilderHighlightType.SOCIAL_IMPACT, BuilderHighlightStatus.PUBLISHED, true, true),
            row(7L, "M3M", BuilderHighlightType.NEWS_ARTICLE, BuilderHighlightStatus.PUBLISHED, true, true),
            row(7L, "M3M", BuilderHighlightType.SFS_ANALYSIS, BuilderHighlightStatus.PUBLISHED, true, true)
        ));

        BuilderHighlightBuilderProgressResponse response = service.getBuilderProgress(7L);

        assertThat(response.getOverallStatus()).isNotEqualTo(BuilderHighlightOverallProgressStatus.READY);
        BuilderHighlightSectionProgressResponse builderUpdate = sectionOf(response, BuilderHighlightType.BUILDER_UPDATE);
        assertThat(builderUpdate.getStatus()).isNotEqualTo(BuilderHighlightSectionStatus.COMPLETE);
    }

    @Test
    void deletedItemsAreExcludedByRepositoryQueryAndDoNotCountTowardProgress() {
        when(builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(builder(7L, "M3M")));
        // The repository query already filters deletedAt IS NULL, so a soft-deleted item
        // simply never appears in the rows returned here.
        when(progressRepository.findProgressRowsByBuilderIds(List.of(7L))).thenReturn(List.of());

        BuilderHighlightBuilderProgressResponse response = service.getBuilderProgress(7L);

        assertThat(response.isHasAnyHighlights()).isFalse();
        assertThat(response.getOverallStatus()).isEqualTo(BuilderHighlightOverallProgressStatus.NOT_STARTED);
    }

    @Test
    void inactiveItemIsIgnoredForPublicReadinessButStillCountsAsPresent() {
        when(builderRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(builder(7L, "M3M")));
        when(progressRepository.findProgressRowsByBuilderIds(List.of(7L))).thenReturn(List.of(
            row(7L, "M3M", BuilderHighlightType.BUILDER_UPDATE, BuilderHighlightStatus.PUBLISHED, true, false)
        ));

        BuilderHighlightBuilderProgressResponse response = service.getBuilderProgress(7L);

        BuilderHighlightSectionProgressResponse builderUpdate = sectionOf(response, BuilderHighlightType.BUILDER_UPDATE);
        assertThat(builderUpdate.isPresent()).isTrue();
        assertThat(builderUpdate.getStatus()).isNotEqualTo(BuilderHighlightSectionStatus.COMPLETE);
        assertThat(builderUpdate.getPublishedPublicItemCount()).isZero();
    }

    @Test
    void overviewAggregatesCountsAcrossAllBuilders() {
        when(builderRepository.countByDeletedFalse()).thenReturn(2L);
        when(progressRepository.findAllProgressRows()).thenReturn(List.of(
            row(1L, "Builder A", BuilderHighlightType.BUILDER_UPDATE, BuilderHighlightStatus.PUBLISHED, true, true),
            row(1L, "Builder A", BuilderHighlightType.SOCIAL_IMPACT, BuilderHighlightStatus.DRAFT, false, true),
            row(2L, "Builder B", BuilderHighlightType.NEWS_ARTICLE, BuilderHighlightStatus.PENDING_REVIEW, false, true)
        ));

        BuilderHighlightOverviewResponse overview = service.getOverview();

        assertThat(overview.getTotalBuilders()).isEqualTo(2L);
        assertThat(overview.getBuildersWithAnyHighlights()).isEqualTo(2L);
        assertThat(overview.getBuildersMissingHighlights()).isZero();
        assertThat(overview.getTotalHighlightItems()).isEqualTo(3L);
        assertThat(overview.getPublishedItems()).isEqualTo(1L);
        assertThat(overview.getDraftItems()).isEqualTo(1L);
        assertThat(overview.getPendingReviewItems()).isEqualTo(1L);
        assertThat(overview.getSectionCoverage()).hasSize(4);
    }

    private BuilderHighlightSectionProgressResponse sectionOf(
        BuilderHighlightBuilderProgressResponse response,
        BuilderHighlightType type
    ) {
        return response.getSections().stream()
            .filter(s -> s.getHighlightType() == type)
            .findFirst()
            .orElseThrow();
    }

    private BuilderEntity builder(Long id, String name) {
        BuilderEntity builder = new BuilderEntity();
        builder.setId(id);
        builder.setName(name);
        return builder;
    }

    private BuilderHighlightProgressRow row(
        Long builderId,
        String builderName,
        BuilderHighlightType type,
        BuilderHighlightStatus status,
        boolean publicVisible,
        boolean active
    ) {
        return new BuilderHighlightProgressRow(
            builderId, builderName, type, status, publicVisible, active, OffsetDateTime.now(), "Sample title"
        );
    }
}
