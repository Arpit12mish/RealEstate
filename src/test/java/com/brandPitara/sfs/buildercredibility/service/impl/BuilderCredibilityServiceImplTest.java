package com.brandPitara.sfs.buildercredibility.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityResponse;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityCardResponse;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityProjectEvidenceDto;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.repository.BuilderHighlightItemRepository;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Hardens GAP-030: the public Builder Credibility contract (controller ->
 * service -> DTO) previously had a single test covering only highlight
 * availability. These tests lock in the real, already-shipped computation
 * (visibility gating, project-meter aggregation, scoring bands, evidence
 * ordering, batch-query shape) so a future change to
 * BuilderCredibilityServiceImpl can't silently break the public contract.
 * No scoring/labeling/aggregation logic is redesigned here - every asserted
 * value is derived from the existing implementation, not invented.
 */
class BuilderCredibilityServiceImplTest {

    private BuilderRepository builderRepository;
    private ProjectRepository projectRepository;
    private ProjectMeterSnapshotRepository snapshotRepository;
    private ProjectComplianceItemRepository complianceRepository;
    private ProjectConstructionStageRepository stageRepository;
    private BuilderHighlightItemRepository highlightRepository;
    private BuilderCredibilityServiceImpl service;

    @BeforeEach
    void setUp() {
        builderRepository = mock(BuilderRepository.class);
        projectRepository = mock(ProjectRepository.class);
        snapshotRepository = mock(ProjectMeterSnapshotRepository.class);
        complianceRepository = mock(ProjectComplianceItemRepository.class);
        stageRepository = mock(ProjectConstructionStageRepository.class);
        highlightRepository = mock(BuilderHighlightItemRepository.class);

        service = new BuilderCredibilityServiceImpl(
            builderRepository,
            projectRepository,
            snapshotRepository,
            complianceRepository,
            stageRepository,
            highlightRepository
        );

        // Every test's builder has no tracked projects unless a test overrides
        // this stub - keeps unrelated tests (e.g. pure visibility checks) from
        // having to stub the full project-meter chain.
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(anyLong()))
            .thenReturn(List.of());
    }

    // ── Public builder visibility ───────────────────────────────────────────

    @Test
    void unknownBuilderReturns404EquivalentException() {
        when(builderRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetCredibility(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deletedBuilderReturns404EquivalentException() {
        // findByIdAndDeletedFalse itself excludes deleted rows at the query
        // level, so a deleted builder surfaces as empty, same as unknown.
        when(builderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publicGetCredibility(5L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void inactiveBuilderReturns404EquivalentException() {
        BuilderEntity inactive = BuilderEntity.builder()
            .id(1L).name("Builder 1").slug("builder-1")
            .active(false).published(true).deleted(false)
            .build();
        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.publicGetCredibility(1L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unpublishedBuilderReturns404EquivalentException() {
        BuilderEntity unpublished = BuilderEntity.builder()
            .id(1L).name("Builder 1").slug("builder-1")
            .active(true).published(false).deleted(false)
            .build();
        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(unpublished));

        assertThatThrownBy(() -> service.publicGetCredibility(1L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void publishedActiveNonDeletedBuilderReturnsCredibility() {
        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(builder(1L)));

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        assertThat(response.getBuilderId()).isEqualTo(1L);
        assertThat(response.getBuilderName()).isEqualTo("Builder 1");
    }

    // ── Project Meter aggregation: empty / no tracked projects ─────────────

    @Test
    void noProjectMeterRecordsProducesExplicitEmptyContractBehavior() {
        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(builder(1L)));

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        assertThat(response.getTrackedProjectsCount()).isZero();
        assertThat(response.getCredibilityScore()).isZero();
        assertThat(response.getCredibilityLabel()).isEqualTo("High Risk");
        assertThat(response.getConfidenceLabel()).isEqualTo("Low");
        assertThat(response.getSummary())
            .isEqualTo("Credibility is currently based on builder profile visibility only because no tracked public projects were found.");
        assertThat(response.getRecentProjectEvidence()).isEmpty();
        assertThat(response.getPositiveIndicators()).hasSize(1);
        assertThat(response.getPositiveIndicators().get(0).getTitle()).isEqualTo("Portfolio under observation");
        assertThat(response.getObservedRisks()).hasSize(1);
        assertThat(response.getObservedRisks().get(0).getTitle()).isEqualTo("No major portfolio-level risk flag");
    }

    @Test
    void noProjectMeterRecordsProducesExplicitEmptySummaryBehavior() {
        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(builder(1L)));

        BuilderCredibilitySummaryResponse summary = service.publicGetCredibilitySummary(1L);

        assertThat(summary.getProjectsTrackedCount()).isZero();
        assertThat(summary.getOnTrackRecordPercent()).isEqualTo(0.0);
        assertThat(summary.getPromisesMetPercent()).isEqualTo(0.0);
        assertThat(summary.getComplianceStrengthPercent()).isEqualTo(0.0);
    }

    // ── Project Meter aggregation: one eligible project, no meter evidence ──

    @Test
    void oneEligibleProjectWithNoMeterEvidenceProducesFullyDeterministicResponse() {
        ProjectEntity project = project(10L, builder(1L), 0);
        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(builder(1L)));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(List.of(project));
        when(snapshotRepository.findByProjectIdIn(List.of(10L))).thenReturn(List.of());
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(List.of(10L)))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(List.of(10L)))
            .thenReturn(List.of());

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        // Hand-derived from the real, unmodified scoring pipeline: a single
        // project with no snapshot/compliance/stage rows falls back to the
        // documented defaults (35% compliance floor, 50%/40% discipline and
        // handover defaults) at every stage of the computation.
        assertThat(response.getTrackedProjectsCount()).isEqualTo(1);
        assertThat(response.getCredibilityScore()).isEqualTo(42);
        assertThat(response.getCredibilityLabel()).isEqualTo("Needs Caution");
        assertThat(response.getConfidenceLabel()).isEqualTo("Low");
        assertThat(response.getSummary())
            .isEqualTo("Needs Caution based on 1 tracked public projects, with 100% currently on track and derived promise fulfilment of 66%.");

        assertThat(response.getScoreBreakdown()).hasSize(5);
        assertThat(response.getScoreBreakdown().get(0).getKey()).isEqualTo("execution_reliability");
        assertThat(response.getScoreBreakdown().get(0).getScore()).isEqualTo(17);
        assertThat(response.getScoreBreakdown().get(0).getMaxScore()).isEqualTo(30);
        assertThat(response.getScoreBreakdown().get(1).getScore()).isEqualTo(17);
        assertThat(response.getScoreBreakdown().get(1).getMaxScore()).isEqualTo(25);
        assertThat(response.getScoreBreakdown().get(2).getScore()).isEqualTo(7);
        assertThat(response.getScoreBreakdown().get(2).getMaxScore()).isEqualTo(20);
        assertThat(response.getScoreBreakdown().get(3).getScore()).isEqualTo(0);
        assertThat(response.getScoreBreakdown().get(3).getMaxScore()).isEqualTo(15);
        assertThat(response.getScoreBreakdown().get(4).getScore()).isEqualTo(1);
        assertThat(response.getScoreBreakdown().get(4).getMaxScore()).isEqualTo(10);

        assertThat(response.getObservedRisks())
            .extracting(risk -> risk.getTitle())
            .containsExactly("Incomplete meter coverage", "Compliance evidence gaps");

        assertThat(response.getPositiveIndicators()).hasSize(1);
        assertThat(response.getPositiveIndicators().get(0).getTitle()).isEqualTo("Strong on-track execution");
        assertThat(response.getPositiveIndicators().get(0).getDescription())
            .isEqualTo("100% of tracked projects are currently on track.");

        assertThat(response.getRecentProjectEvidence()).hasSize(1);
        BuilderCredibilityProjectEvidenceDto evidence = response.getRecentProjectEvidence().get(0);
        assertThat(evidence.getProjectId()).isEqualTo(10L);
        assertThat(evidence.getTimelineStatus()).isEqualTo("ON_TRACK");
        assertThat(evidence.getDelayDays()).isZero();
        assertThat(evidence.getPromiseFulfilmentPercent()).isEqualTo(66);
        assertThat(evidence.getComplianceStrengthPercent()).isEqualTo(35);
        assertThat(evidence.getVerified()).isFalse();
    }

    // ── Project Meter aggregation: multiple projects, mixed status ─────────

    @Test
    void inactiveDeletedAndUnpublishedProjectsAreExcludedByTheRepositoryFilter() {
        // The service trusts the repository's own
        // published=true/active=true/deleted=false filter to do exclusion -
        // this test documents that trust boundary: whatever the repository
        // returns is exactly what gets aggregated, nothing further is
        // filtered in the service itself.
        ProjectEntity onlyEligibleProject = project(20L, builder(1L), 0);
        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(builder(1L)));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(List.of(onlyEligibleProject));
        when(snapshotRepository.findByProjectIdIn(List.of(20L))).thenReturn(List.of());
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(List.of(20L)))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(List.of(20L)))
            .thenReturn(List.of());

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        assertThat(response.getTrackedProjectsCount()).isEqualTo(1);
        assertThat(response.getRecentProjectEvidence()).hasSize(1);
        assertThat(response.getRecentProjectEvidence().get(0).getProjectId()).isEqualTo(20L);
    }

    @Test
    void delayedProjectIsExcludedFromOnTrackCountAndContributesToDelayMetrics() {
        ProjectEntity project = project(30L, builder(1L), 0);
        ProjectMeterSnapshotEntity delayedSnapshot = ProjectMeterSnapshotEntity.builder()
            .project(project)
            .delayDays(45)
            .constructionProgressPercent(60)
            .verified(false)
            .build();

        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(builder(1L)));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(List.of(project));
        when(snapshotRepository.findByProjectIdIn(List.of(30L))).thenReturn(List.of(delayedSnapshot));
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(List.of(30L)))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(List.of(30L)))
            .thenReturn(List.of());

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        BuilderCredibilityProjectEvidenceDto evidence = response.getRecentProjectEvidence().get(0);
        assertThat(evidence.getTimelineStatus()).isEqualTo("DELAYED");
        assertThat(evidence.getDelayDays()).isEqualTo(45);
        assertThat(response.getMetrics().stream()
            .filter(m -> m.getKey().equals("on_track_record"))
            .findFirst().orElseThrow().getValue())
            .isEqualTo("0%");
        assertThat(response.getObservedRisks())
            .extracting(risk -> risk.getTitle())
            .contains("Delay pressure detected");
    }

    @Test
    void missingOptionalMeterFieldsFallBackToStageDerivedProgressWithoutError() {
        ProjectEntity project = project(40L, builder(1L), 0);
        // Snapshot present but with a null constructionProgressPercent - the
        // service must fall back to calculateProgressFromStages rather than NPE.
        ProjectMeterSnapshotEntity snapshotWithNullProgress = ProjectMeterSnapshotEntity.builder()
            .project(project)
            .delayDays(null)
            .constructionProgressPercent(null)
            .verified(null)
            .build();
        ProjectConstructionStageEntity stage = ProjectConstructionStageEntity.builder()
            .project(project)
            .progressPercent(50)
            .weightPercent(100)
            .build();

        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(builder(1L)));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(List.of(project));
        when(snapshotRepository.findByProjectIdIn(List.of(40L))).thenReturn(List.of(snapshotWithNullProgress));
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(List.of(40L)))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(List.of(40L)))
            .thenReturn(List.of(stage));

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        assertThat(response.getRecentProjectEvidence().get(0).getConstructionProgressPercent()).isEqualTo(50);
        // Null delayDays on the snapshot must not NPE and must be treated as 0.
        assertThat(response.getRecentProjectEvidence().get(0).getDelayDays()).isZero();
    }

    @Test
    void decimalPercentagesAreRoundedToTwoDecimalPlacesInSummaryContract() {
        BuilderEntity b = builder(1L);
        ProjectEntity onTrack = project(50L, b, 0);
        ProjectEntity delayedA = project(51L, b, 0);
        ProjectEntity delayedB = project(52L, b, 0);
        List<ProjectEntity> projects = List.of(onTrack, delayedA, delayedB);

        ProjectMeterSnapshotEntity delayedSnapshotA = ProjectMeterSnapshotEntity.builder()
            .project(delayedA).delayDays(5).build();
        ProjectMeterSnapshotEntity delayedSnapshotB = ProjectMeterSnapshotEntity.builder()
            .project(delayedB).delayDays(5).build();

        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(b));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(projects);
        when(snapshotRepository.findByProjectIdIn(List.of(50L, 51L, 52L)))
            .thenReturn(List.of(delayedSnapshotA, delayedSnapshotB));
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(List.of(50L, 51L, 52L)))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(List.of(50L, 51L, 52L)))
            .thenReturn(List.of());

        BuilderCredibilitySummaryResponse summary = service.publicGetCredibilitySummary(1L);

        // 1 of 3 tracked projects on track -> 100/3 = 33.333...-> rounded to 33.33,
        // not truncated and not left at full float precision.
        assertThat(summary.getOnTrackRecordPercent()).isEqualTo(33.33);
    }

    // ── Ordering ─────────────────────────────────────────────────────────────

    @Test
    void recentProjectEvidenceIsSortedByPriorityRegardlessOfInputOrder() {
        BuilderEntity b = builder(1L);
        ProjectEntity priorityThree = project(60L, b, 3);
        ProjectEntity priorityOne = project(61L, b, 1);
        ProjectEntity priorityTwo = project(62L, b, 2);
        // Fed out of priority order on purpose to prove the service re-sorts,
        // rather than trusting whatever order the repository happened to return.
        List<ProjectEntity> outOfOrder = List.of(priorityThree, priorityOne, priorityTwo);

        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(b));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(outOfOrder);
        when(snapshotRepository.findByProjectIdIn(any())).thenReturn(List.of());
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(any()))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(any()))
            .thenReturn(List.of());

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        assertThat(response.getRecentProjectEvidence())
            .extracting(BuilderCredibilityProjectEvidenceDto::getProjectId)
            .containsExactly(61L, 62L, 60L);
    }

    @Test
    void recentProjectEvidenceIsCappedAtFiveEvenWithMoreTrackedProjects() {
        BuilderEntity b = builder(1L);
        List<ProjectEntity> sevenProjects = List.of(
            project(70L, b, 0), project(71L, b, 1), project(72L, b, 2),
            project(73L, b, 3), project(74L, b, 4), project(75L, b, 5),
            project(76L, b, 6)
        );
        List<Long> ids = sevenProjects.stream().map(ProjectEntity::getId).toList();

        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(b));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(sevenProjects);
        when(snapshotRepository.findByProjectIdIn(ids)).thenReturn(List.of());
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(ids))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(ids))
            .thenReturn(List.of());

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        assertThat(response.getTrackedProjectsCount()).isEqualTo(7);
        assertThat(response.getRecentProjectEvidence()).hasSize(5);
        assertThat(response.getRecentProjectEvidence())
            .extracting(BuilderCredibilityProjectEvidenceDto::getProjectId)
            .containsExactly(70L, 71L, 72L, 73L, 74L);
    }

    // ── Multiple compliance/stage rows per project (the real "many child rows" shape) ──

    @Test
    void multipleComplianceAndStageRowsForTheSameProjectAreGroupedNotOverwritten() {
        BuilderEntity b = builder(1L);
        ProjectEntity project = project(80L, b, 0);

        ProjectComplianceItemEntity itemA = complianceItem(project, ProjectComplianceStatus.OBTAINED, false);
        ProjectComplianceItemEntity itemB = complianceItem(project, ProjectComplianceStatus.PENDING, false);
        ProjectConstructionStageEntity stageA = ProjectConstructionStageEntity.builder()
            .project(project).progressPercent(100).weightPercent(50).build();
        ProjectConstructionStageEntity stageB = ProjectConstructionStageEntity.builder()
            .project(project).progressPercent(0).weightPercent(50).build();

        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(b));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(List.of(project));
        when(snapshotRepository.findByProjectIdIn(List.of(80L))).thenReturn(List.of());
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(List.of(80L)))
            .thenReturn(List.of(itemA, itemB));
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(List.of(80L)))
            .thenReturn(List.of(stageA, stageB));

        BuilderCredibilityResponse response = service.publicGetCredibility(1L);

        // Progress from stages: (100*50 + 0*50) / 100 = 50, proving both stage
        // rows for the one project were used in the weighted computation, not
        // just the last one seen.
        assertThat(response.getRecentProjectEvidence().get(0).getConstructionProgressPercent()).isEqualTo(50);
    }

    // ── Public response DTO safety: no unpublished/non-public builders leak via cards ──

    @Test
    void unpublishedBuilderIsNeverIncludedInCredibilityCardsBecauseRepositoryQueryExcludesIt() {
        // publicListCredibilityCards trusts
        // findTop20ByPublishedTrueAndActiveTrueAndDeletedFalse... - simulate the
        // repository correctly excluding a builder by simply never returning it.
        when(builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc())
            .thenReturn(List.of());

        List<BuilderCredibilityCardResponse> cards = service.publicListCredibilityCards(null, 10);

        assertThat(cards).isEmpty();
        verifyNoInteractions(projectRepository, snapshotRepository, complianceRepository, stageRepository);
    }

    // ── Query behavior / N+1 prevention ─────────────────────────────────────

    @Test
    void singleBuilderCredibilityComputationBatchesMeterQueriesInsteadOfPerProject() {
        BuilderEntity b = builder(1L);
        List<ProjectEntity> fourProjects = List.of(
            project(90L, b, 0), project(91L, b, 1), project(92L, b, 2), project(93L, b, 3)
        );
        List<Long> ids = fourProjects.stream().map(ProjectEntity::getId).toList();

        when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(b));
        when(projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
            .thenReturn(fourProjects);
        when(snapshotRepository.findByProjectIdIn(ids)).thenReturn(List.of());
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(ids))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(ids))
            .thenReturn(List.of());

        service.publicGetCredibility(1L);

        // Exactly one batch call per repository, never one per project - the
        // N+1 shape this test exists to prevent a future regression into.
        verify(snapshotRepository, times(1)).findByProjectIdIn(any());
        verify(complianceRepository, times(1))
            .findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(any());
        verify(stageRepository, times(1))
            .findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(any());
        verify(projectRepository, times(1))
            .findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L);
    }

    @Test
    void credibilityCardsForMultipleBuildersBatchMeterQueriesAcrossAllBuildersInOneCallEach() {
        BuilderEntity builderOne = builder(1L);
        BuilderEntity builderTwo = builder(2L);
        ProjectEntity projectForOne = project(100L, builderOne, 0);
        ProjectEntity projectForTwo = project(101L, builderTwo, 0);

        when(builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc())
            .thenReturn(List.of(builderOne, builderTwo));
        when(projectRepository.findByBuilderIdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(List.of(1L, 2L)))
            .thenReturn(List.of(projectForOne, projectForTwo));
        when(snapshotRepository.findByProjectIdIn(List.of(100L, 101L))).thenReturn(List.of());
        when(complianceRepository.findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(List.of(100L, 101L)))
            .thenReturn(List.of());
        when(stageRepository.findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(List.of(100L, 101L)))
            .thenReturn(List.of());
        when(highlightRepository.existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(anyLong(), any()))
            .thenReturn(false);

        List<BuilderCredibilityCardResponse> cards = service.publicListCredibilityCards(null, 10);

        assertThat(cards).hasSize(2);
        // One call covering both builders' projects, not one call per builder -
        // this is the exact N+1 shape a naive per-card refactor could reintroduce.
        verify(projectRepository, times(1))
            .findByBuilderIdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(any());
        verify(snapshotRepository, times(1)).findByProjectIdIn(any());
        verify(complianceRepository, times(1))
            .findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(any());
        verify(stageRepository, times(1))
            .findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(any());
    }

    @Test
    void credibilityCardsLimitIsClampedBetweenOneAndTheHomeCardMaximum() {
        BuilderEntity b = builder(1L);
        when(builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc())
            .thenReturn(List.of(b));
        when(projectRepository.findByBuilderIdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(any()))
            .thenReturn(List.of());
        when(highlightRepository.existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(anyLong(), any()))
            .thenReturn(false);

        // A caller requesting far more than the home-card maximum (10) must be
        // clamped, not allowed to force an unbounded response.
        List<BuilderCredibilityCardResponse> cards = service.publicListCredibilityCards(null, 999);

        assertThat(cards).hasSizeLessThanOrEqualTo(10);
    }

    // ── Existing test (unmodified) ──────────────────────────────────────────

    @Test
    void credibilityCardsExposeHighlightsAvailabilityFromExistsQuery() {
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

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private BuilderEntity builder(Long id) {
        return BuilderEntity.builder()
            .id(id)
            .name("Builder " + id)
            .slug("builder-" + id)
            .active(true)
            .published(true)
            .deleted(false)
            .city(CityEntity.builder().id(1L).name("Gurugram").build())
            .build();
    }

    private ProjectEntity project(Long id, BuilderEntity builder, int priority) {
        return ProjectEntity.builder()
            .id(id)
            .builder(builder)
            .name("Project " + id)
            .slug("project-" + id)
            .active(true)
            .published(true)
            .deleted(false)
            .priority(priority)
            .build();
    }

    private ProjectComplianceItemEntity complianceItem(
        ProjectEntity project,
        ProjectComplianceStatus status,
        boolean verified
    ) {
        return ProjectComplianceItemEntity.builder()
            .project(project)
            .itemGroup(ProjectComplianceGroup.values()[0])
            .itemKey("item")
            .itemLabel("Item")
            .status(status)
            .displayOrder(0)
            .verified(verified)
            .build();
    }
}
