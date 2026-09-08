package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanInsightDetailResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisEntity;
import com.brandPitara.sfs.project.enums.FloorPlanRoomType;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectFloorPlanInsightServiceImplTest {

  private static final Long PROJECT_ID = 123L;
  private static final Long FLOOR_PLAN_ID = 991L;

  private ProjectRepository projectRepository;
  private ProjectFloorPlanRepository floorPlanRepository;
  private ProjectFloorPlanInsightRepository insightRepository;
  private ProjectFloorPlanRoomDimensionRepository roomRepository;
  private ProjectFloorPlanVisualAnalysisRepository visualAnalysisRepository;
  private ProjectPublicVisibilityPolicy visibilityPolicy;
  private ProjectFloorPlanInsightServiceImpl service;

  @BeforeEach
  void setUp() {
    projectRepository = mock(ProjectRepository.class);
    floorPlanRepository = mock(ProjectFloorPlanRepository.class);
    insightRepository = mock(ProjectFloorPlanInsightRepository.class);
    roomRepository = mock(ProjectFloorPlanRoomDimensionRepository.class);
    visualAnalysisRepository = mock(ProjectFloorPlanVisualAnalysisRepository.class);
    visibilityPolicy = mock(ProjectPublicVisibilityPolicy.class);
    ContentVersionService contentVersionService = mock(ContentVersionService.class);

    service = new ProjectFloorPlanInsightServiceImpl(
        projectRepository, floorPlanRepository, insightRepository, roomRepository,
        visualAnalysisRepository, contentVersionService, visibilityPolicy,
        mock(ProjectPublicCacheEvictionPublisher.class)
    );

    ProjectEntity project = ProjectEntity.builder().id(PROJECT_ID).build();
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    // Explicit anyLong() disambiguates against the assertPubliclyVisible(entity, slug)
    // overload added for GAP-001 (Phase 4A) - both overloads previously matched a bare
    // any(), any() stubbing here once a second overload existed.
    doNothing().when(visibilityPolicy).assertPubliclyVisible(any(), anyLong());

    ProjectFloorPlanEntity floorPlan = ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID)
        .project(project)
        .title("3 BHK Type A")
        .active(true)
        .build();
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    when(insightRepository.findByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of());
  }

  @Test
  void marksResponseAsDemoWhenNoRoomComparisonAndNoVisualAnalysisAuthored() {
    ProjectFloorPlanRoomDimensionEntity room = ProjectFloorPlanRoomDimensionEntity.builder()
        .roomType(FloorPlanRoomType.MASTER_BEDROOM)
        .dimensionText("12ft * 14ft")
        .build(); // no averageAreaSqft -> no comparison data
    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of(room));
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.empty());

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.isDemo()).isTrue();
    assertThat(response.getSourceLabel()).isEqualTo("Sample content");
    assertThat(response.getVisualAnalysis()).isNull();
  }

  @Test
  void notDemoWhenAtLeastOneRoomHasAuthoredComparisonData() {
    ProjectFloorPlanRoomDimensionEntity room = ProjectFloorPlanRoomDimensionEntity.builder()
        .roomType(FloorPlanRoomType.MASTER_BEDROOM)
        .dimensionText("12ft * 14ft")
        .areaSqft(BigDecimal.valueOf(168))
        .averageAreaSqft(BigDecimal.valueOf(149))
        .differencePercent(BigDecimal.valueOf(13))
        .comparisonSummary("13% bigger than average.")
        .build();
    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of(room));
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.empty());

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.isDemo()).isFalse();
    assertThat(response.getSourceLabel()).isEqualTo("Verified floor-plan intelligence");
    assertThat(response.getRooms()).hasSize(1);
    assertThat(response.getRooms().get(0).getComparisonLabel()).isEqualTo("+13% from avg");
  }

  @Test
  void notDemoWhenVisualAnalysisAuthoredEvenWithoutRoomComparisonData() {
    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of());
    ProjectFloorPlanVisualAnalysisEntity visualAnalysis = ProjectFloorPlanVisualAnalysisEntity.builder()
        .title("Visual analysis of every important factor")
        .mediaUrl("https://cdn.example.com/insight.webp")
        .build();
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(visualAnalysis));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.isDemo()).isFalse();
    assertThat(response.getVisualAnalysis()).isNotNull();
    assertThat(response.getVisualAnalysis().getMediaUrl()).isEqualTo("https://cdn.example.com/insight.webp");
  }
}
