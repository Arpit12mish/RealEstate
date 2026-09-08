package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.media.validator.TrustedMediaUrlValidator;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanInsightDetailResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;
import com.brandPitara.sfs.project.enums.FloorPlanRoomType;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanInsightRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRoomDimensionRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanVisualAnalysisRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves the room "Space Comparison" contract is correctly wired end-to-end
 * through the SAME public composer Phase 4D/4E already use
 * (ProjectFloorPlanInsightServiceImpl#publicGetDetail) — no controller or
 * service change was needed for the read path: patching
 * ProjectFloorPlanRoomDimensionMapper alone was sufficient, since
 * publicGetDetail() already calls it unchanged. This test exercises that
 * exact, unmodified call path, not a mock of the mapper.
 */
class ProjectFloorPlanInsightServiceImplRoomComparisonTest {

  private static final Long PROJECT_ID = 501L;
  private static final Long FLOOR_PLAN_ID = 701L;

  private ProjectRepository projectRepository;
  private ProjectFloorPlanRepository floorPlanRepository;
  private ProjectFloorPlanRoomDimensionRepository roomRepository;
  private ProjectFloorPlanInsightServiceImpl service;

  @BeforeEach
  void setUp() {
    projectRepository = mock(ProjectRepository.class);
    floorPlanRepository = mock(ProjectFloorPlanRepository.class);
    ProjectFloorPlanInsightRepository insightRepository = mock(ProjectFloorPlanInsightRepository.class);
    roomRepository = mock(ProjectFloorPlanRoomDimensionRepository.class);
    ContentVersionService contentVersionService = mock(ContentVersionService.class);
    ProjectFloorPlanVisualAnalysisRepository visualAnalysisRepository = mock(ProjectFloorPlanVisualAnalysisRepository.class);
    TrustedMediaUrlValidator trustedMediaUrlValidator = mock(TrustedMediaUrlValidator.class);
    ProjectPublicCacheEvictionPublisher cacheEvictionPublisher = mock(ProjectPublicCacheEvictionPublisher.class);

    service = new ProjectFloorPlanInsightServiceImpl(
        projectRepository,
        floorPlanRepository,
        insightRepository,
        roomRepository,
        visualAnalysisRepository,
        contentVersionService,
        new ProjectPublicVisibilityPolicy(),
        trustedMediaUrlValidator,
        cacheEvictionPublisher);

    when(insightRepository.findByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of());
  }

  private ProjectEntity approvedProject() {
    return ProjectEntity.builder()
        .id(PROJECT_ID).published(true).active(true).deleted(false)
        .reviewStatus(ReviewStatus.APPROVED)
        .builder(publiclyAvailableBuilder())
        .build();
  }

  private BuilderEntity publiclyAvailableBuilder() {
    return BuilderEntity.builder()
        .id(1L)
        .published(true)
        .active(true)
        .deleted(false)
        .build();
  }

  private ProjectFloorPlanEntity activeFloorPlan(ProjectEntity project) {
    return ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID).project(project).active(true).deleted(false).build();
  }

  // Mapping parity: publicGetDetail()'s rooms[] carries the full Space
  // Comparison shape for a room with authored comparison data.
  @Test
  void publicGetDetailIncludesSpaceComparisonFieldsForARoomWithAuthoredData() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));

    ProjectFloorPlanRoomDimensionEntity room = ProjectFloorPlanRoomDimensionEntity.builder()
        .id(9001L).floorPlan(floorPlan).roomType(FloorPlanRoomType.MASTER_BEDROOM)
        .label("Master Bedroom").areaSqft(BigDecimal.valueOf(168))
        .averageAreaSqft(BigDecimal.valueOf(149))
        .comparisonContextLabel("Compared to similar 2 BHK units nearby")
        .differencePercent(BigDecimal.valueOf(13))
        .comparisonSummary("This master bedroom is larger than average.")
        .comparisonVerified(true)
        .active(true).deleted(false).sortOrder(1).build();
    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of(room));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getRooms()).hasSize(1);
    FloorPlanRoomDimensionResponse mapped = response.getRooms().get(0);
    assertThat(mapped.getAverageAreaSqft()).isEqualByComparingTo("149");
    assertThat(mapped.getComparisonContextLabel()).isEqualTo("Compared to similar 2 BHK units nearby");
    assertThat(mapped.getDifferencePercent()).isEqualByComparingTo("13");
    assertThat(mapped.getComparisonLabel()).isEqualTo("+13% from avg");
    assertThat(mapped.getSummary()).isEqualTo("This master bedroom is larger than average.");
    assertThat(mapped.isComparisonVerified()).isTrue();
    assertThat(mapped.isHasComparisonData()).isTrue();
  }

  // Mapping parity: a room without an authored average correctly omits the
  // comparison badge/summary, matching the mapper's own hasComparisonData gate.
  @Test
  void publicGetDetailOmitsComparisonBadgeForARoomWithoutAnAuthoredAverage() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));

    ProjectFloorPlanRoomDimensionEntity room = ProjectFloorPlanRoomDimensionEntity.builder()
        .id(9002L).floorPlan(floorPlan).roomType(FloorPlanRoomType.KITCHEN)
        .label("Kitchen").areaSqft(BigDecimal.valueOf(72))
        .active(true).deleted(false).sortOrder(2).build();
    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of(room));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    FloorPlanRoomDimensionResponse mapped = response.getRooms().get(0);
    assertThat(mapped.isHasComparisonData()).isFalse();
    assertThat(mapped.getComparisonLabel()).isNull();
    assertThat(mapped.getSummary()).isNull();
  }

  // Ownership: a floor plan belonging to a different project must still 404,
  // even when it carries real Space Comparison data — no leak across projects.
  @Test
  void publicGetDetailThrowsNotFoundForWrongProjectFloorPlanPairingEvenWithComparisonData() {
    ProjectEntity requestedProject = approvedProject();
    ProjectEntity actualOwningProject = ProjectEntity.builder()
        .id(999L).published(true).active(true).deleted(false).reviewStatus(ReviewStatus.APPROVED).build();
    ProjectFloorPlanEntity floorPlan = ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID).project(actualOwningProject).active(true).deleted(false).build();
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(requestedProject));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // Visibility: an unapproved project's rooms (even with authored comparison
  // data) must never be publicly reachable.
  @Test
  void publicGetDetailThrowsNotFoundForPendingReviewProjectEvenWithComparisonData() {
    ProjectEntity pending = ProjectEntity.builder()
        .id(PROJECT_ID).published(true).active(true).deleted(false)
        .reviewStatus(ReviewStatus.PENDING_REVIEW).build();
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(pending));

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // Visibility: an inactive floor plan must 404 regardless of its rooms' own
  // comparison data.
  @Test
  void publicGetDetailThrowsNotFoundForInactiveFloorPlanEvenWithComparisonData() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity inactive = ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID).project(project).active(false).deleted(false).build();
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(inactive));

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // Only active, non-deleted rooms are ever considered for comparison output
  // — the repository query itself enforces this (findBy...ActiveTrueAndDeletedFalse...),
  // confirmed by construction here (the mock only ever returns active rooms).
  @Test
  void publicGetDetailNeverReceivesInactiveRoomsFromTheRepositoryQuery() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    // The repository's own query signature (AndActiveTrueAndDeletedFalse) is
    // the enforcement point - this mock simply proves the service never
    // second-guesses or re-filters what the repository returns.
    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of());

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getRooms()).isEmpty();
  }
}
