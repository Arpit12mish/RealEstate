package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.project.dto.FloorPlanInsightResponse;
import com.brandPitara.sfs.project.dto.FloorPlanInsightUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanInsightEntity;
import com.brandPitara.sfs.project.enums.FloorPlanInsightType;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression coverage that create()/update() actually read every field off
 * FloorPlanInsightUpsertRequest and persist it - specifically dimensionText,
 * added for the Insights tab form. The Room-dimension equivalent of this bug
 * (fields present on the DTO but never mapped in the service) previously
 * shipped silently; this locks in the fix for the Insight side too.
 */
class ProjectFloorPlanInsightCreateUpdateTest {

  private static final Long PROJECT_ID = 123L;
  private static final Long FLOOR_PLAN_ID = 991L;

  private ProjectFloorPlanRepository floorPlanRepository;
  private ProjectFloorPlanInsightRepository insightRepository;
  private ProjectFloorPlanInsightServiceImpl service;

  @BeforeEach
  void setUp() {
    ProjectRepository projectRepository = mock(ProjectRepository.class);
    floorPlanRepository = mock(ProjectFloorPlanRepository.class);
    insightRepository = mock(ProjectFloorPlanInsightRepository.class);
    ProjectFloorPlanRoomDimensionRepository roomRepository = mock(ProjectFloorPlanRoomDimensionRepository.class);
    ProjectFloorPlanVisualAnalysisRepository visualAnalysisRepository = mock(ProjectFloorPlanVisualAnalysisRepository.class);
    ContentVersionService contentVersionService = mock(ContentVersionService.class);
    ProjectPublicVisibilityPolicy visibilityPolicy = mock(ProjectPublicVisibilityPolicy.class);

    service = new ProjectFloorPlanInsightServiceImpl(
        projectRepository, floorPlanRepository, insightRepository, roomRepository,
        visualAnalysisRepository, contentVersionService, visibilityPolicy,
        mock(ProjectPublicCacheEvictionPublisher.class)
    );

    ProjectFloorPlanEntity floorPlan = ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID)
        .project(ProjectEntity.builder().id(PROJECT_ID).build())
        .build();
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    when(insightRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void createPersistsDimensionTextAndBenchmarkFields() {
    FloorPlanInsightUpsertRequest request = FloorPlanInsightUpsertRequest.builder()
        .insightType(FloorPlanInsightType.MASTER_BEDROOM_SIZE)
        .title("Master Bedroom")
        .dimensionText("12ft * 14ft")
        .unitValue(BigDecimal.valueOf(168))
        .benchmarkValue(BigDecimal.valueOf(149))
        .unitLabel("sq ft")
        .differencePercent(BigDecimal.valueOf(13))
        .summary("An average master bedroom in Varthur measures 149 sq ft. This unit's master bedroom is 13% bigger.")
        .verified(true)
        .publicVisible(true)
        .sortOrder(1)
        .build();

    FloorPlanInsightResponse response = service.create(PROJECT_ID, FLOOR_PLAN_ID, request);

    assertThat(response.getDimensionText()).isEqualTo("12ft * 14ft");
    assertThat(response.getUnitValue()).isEqualByComparingTo("168");
    assertThat(response.getBenchmarkValue()).isEqualByComparingTo("149");
    assertThat(response.getUnitLabel()).isEqualTo("sq ft");
    assertThat(response.getDifferencePercent()).isEqualByComparingTo("13");
    assertThat(response.isVerified()).isTrue();
  }

  @Test
  void updatePersistsDimensionTextOntoExistingEntity() {
    ProjectFloorPlanInsightEntity existing = ProjectFloorPlanInsightEntity.builder()
        .id(5L)
        .floorPlan(ProjectFloorPlanEntity.builder().id(FLOOR_PLAN_ID).build())
        .insightType(FloorPlanInsightType.KITCHEN_SIZE)
        .title("Kitchen")
        .build();
    when(insightRepository.findByIdAndFloorPlanId(5L, FLOOR_PLAN_ID)).thenReturn(Optional.of(existing));

    FloorPlanInsightUpsertRequest request = FloorPlanInsightUpsertRequest.builder()
        .dimensionText("13ft * 26.11ft")
        .build();

    FloorPlanInsightResponse response = service.update(PROJECT_ID, FLOOR_PLAN_ID, 5L, request);

    assertThat(response.getDimensionText()).isEqualTo("13ft * 26.11ft");
  }

  @Test
  void customOtherTypeInsightSavesFreeformTitle() {
    FloorPlanInsightUpsertRequest request = FloorPlanInsightUpsertRequest.builder()
        .insightType(FloorPlanInsightType.OTHER)
        .title("Balcony usability score")
        .build();

    FloorPlanInsightResponse response = service.create(PROJECT_ID, FLOOR_PLAN_ID, request);

    assertThat(response.getInsightType()).isEqualTo(FloorPlanInsightType.OTHER);
    assertThat(response.getTitle()).isEqualTo("Balcony usability score");
  }
}
