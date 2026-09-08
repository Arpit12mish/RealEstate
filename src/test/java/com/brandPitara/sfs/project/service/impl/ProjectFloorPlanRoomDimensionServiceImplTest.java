package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;
import com.brandPitara.sfs.project.enums.FloorPlanRoomType;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRoomDimensionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for a bug where averageAreaSqft/comparisonContextLabel/
 * differencePercent/comparisonSummary/comparisonVerified were added to the
 * entity, request, and response DTOs but never actually read off the request
 * in create()/update() - the fields silently never persisted despite the API
 * surface accepting them.
 */
class ProjectFloorPlanRoomDimensionServiceImplTest {

  private static final Long PROJECT_ID = 123L;
  private static final Long FLOOR_PLAN_ID = 991L;

  private ProjectFloorPlanRepository floorPlanRepository;
  private ProjectFloorPlanRoomDimensionRepository roomRepository;
  private ProjectFloorPlanRoomDimensionServiceImpl service;

  @BeforeEach
  void setUp() {
    floorPlanRepository = mock(ProjectFloorPlanRepository.class);
    roomRepository = mock(ProjectFloorPlanRoomDimensionRepository.class);
    ContentVersionService contentVersionService = mock(ContentVersionService.class);
    service = new ProjectFloorPlanRoomDimensionServiceImpl(floorPlanRepository, roomRepository, contentVersionService);

    ProjectFloorPlanEntity floorPlan = ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID)
        .project(ProjectEntity.builder().id(PROJECT_ID).build())
        .build();
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void createPersistsComparisonFieldsOntoTheEntity() {
    FloorPlanRoomDimensionUpsertRequest request = FloorPlanRoomDimensionUpsertRequest.builder()
        .roomType(FloorPlanRoomType.MASTER_BEDROOM)
        .areaSqft(BigDecimal.valueOf(168))
        .dimensionText("12ft * 14ft")
        .averageAreaSqft(BigDecimal.valueOf(149))
        .comparisonContextLabel("Varthur")
        .differencePercent(BigDecimal.valueOf(13))
        .comparisonSummary("An average master bedroom in Varthur measures 149 sq ft. This unit's master bedroom is 13% bigger.")
        .comparisonVerified(true)
        .build();

    FloorPlanRoomDimensionResponse response = service.create(PROJECT_ID, FLOOR_PLAN_ID, request);

    assertThat(response.getAverageAreaSqft()).isEqualByComparingTo("149");
    assertThat(response.getComparisonContextLabel()).isEqualTo("Varthur");
    assertThat(response.getDifferencePercent()).isEqualByComparingTo("13");
    assertThat(response.getSummary()).contains("13% bigger");
    assertThat(response.isComparisonVerified()).isTrue();
    assertThat(response.isHasComparisonData()).isTrue();
    assertThat(response.getComparisonLabel()).isEqualTo("+13% from avg");
  }

  @Test
  void updatePersistsComparisonFieldsOntoTheExistingEntity() {
    ProjectFloorPlanRoomDimensionEntity existing = ProjectFloorPlanRoomDimensionEntity.builder()
        .id(5L)
        .floorPlan(ProjectFloorPlanEntity.builder().id(FLOOR_PLAN_ID).build())
        .roomType(FloorPlanRoomType.KITCHEN)
        .build();
    when(roomRepository.findByIdAndFloorPlanIdAndDeletedFalse(5L, FLOOR_PLAN_ID))
        .thenReturn(Optional.of(existing));

    FloorPlanRoomDimensionUpsertRequest request = FloorPlanRoomDimensionUpsertRequest.builder()
        .roomType(FloorPlanRoomType.KITCHEN)
        .averageAreaSqft(BigDecimal.valueOf(130))
        .comparisonContextLabel("Whitefield")
        .differencePercent(BigDecimal.valueOf(-8))
        .comparisonSummary("Slightly smaller than average.")
        .comparisonVerified(true)
        .build();

    FloorPlanRoomDimensionResponse response = service.update(PROJECT_ID, FLOOR_PLAN_ID, 5L, request);

    assertThat(response.getAverageAreaSqft()).isEqualByComparingTo("130");
    assertThat(response.getComparisonContextLabel()).isEqualTo("Whitefield");
    assertThat(response.getDifferencePercent()).isEqualByComparingTo("-8");
    assertThat(response.getSummary()).isEqualTo("Slightly smaller than average.");
    assertThat(response.isComparisonVerified()).isTrue();
    assertThat(response.getComparisonLabel()).isEqualTo("-8% from avg");
  }
}
