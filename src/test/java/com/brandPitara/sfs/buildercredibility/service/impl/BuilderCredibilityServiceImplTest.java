package com.brandPitara.sfs.buildercredibility.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.repository.BuilderHighlightItemRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BuilderCredibilityServiceImplTest {

    @Test
    void credibilityCardsExposeHighlightsAvailabilityFromExistsQuery() {
        BuilderRepository builderRepository = mock(BuilderRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectMeterSnapshotRepository snapshotRepository = mock(ProjectMeterSnapshotRepository.class);
        ProjectComplianceItemRepository complianceRepository = mock(ProjectComplianceItemRepository.class);
        ProjectConstructionStageRepository stageRepository = mock(ProjectConstructionStageRepository.class);
        BuilderHighlightItemRepository highlightRepository = mock(BuilderHighlightItemRepository.class);

        BuilderCredibilityServiceImpl service = new BuilderCredibilityServiceImpl(
            builderRepository,
            projectRepository,
            snapshotRepository,
            complianceRepository,
            stageRepository,
            highlightRepository
        );

        when(builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc())
            .thenReturn(List.of(builder(1L), builder(2L)));
        when(projectRepository.findByBuilderIdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(List.of(1L, 2L)))
            .thenReturn(List.of());
        when(highlightRepository.existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            1L,
            BuilderHighlightStatus.PUBLISHED
        )).thenReturn(true);
        when(highlightRepository.existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            2L,
            BuilderHighlightStatus.PUBLISHED
        )).thenReturn(false);

        var cards = service.publicListCredibilityCards(null, 10);

        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).getHighlightsAvailable()).isTrue();
        assertThat(cards.get(0).getHighlightCtaLabel()).isEqualTo("Highlights");
        assertThat(cards.get(1).getHighlightsAvailable()).isFalse();
        verify(highlightRepository).existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            1L,
            BuilderHighlightStatus.PUBLISHED
        );
        verify(highlightRepository).existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
            2L,
            BuilderHighlightStatus.PUBLISHED
        );
    }

    private BuilderEntity builder(Long id) {
        return BuilderEntity.builder()
            .id(id)
            .name("Builder " + id)
            .active(true)
            .published(true)
            .deleted(false)
            .build();
    }
}
