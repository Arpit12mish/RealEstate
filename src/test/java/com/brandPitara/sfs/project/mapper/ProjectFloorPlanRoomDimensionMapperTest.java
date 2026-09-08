package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;
import com.brandPitara.sfs.project.enums.FloorPlanRoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectFloorPlanRoomDimensionMapperTest {

  @Test
  void roomWithAuthoredComparisonDataProducesPositiveBadgeLabel() {
    ProjectFloorPlanRoomDimensionEntity entity = ProjectFloorPlanRoomDimensionEntity.builder()
        .id(1L)
        .floorPlan(ProjectFloorPlanEntity.builder().id(991L).build())
        .roomType(FloorPlanRoomType.MASTER_BEDROOM)
        .dimensionText("12ft * 14ft")
        .areaSqft(BigDecimal.valueOf(168))
        .averageAreaSqft(BigDecimal.valueOf(149))
        .differencePercent(BigDecimal.valueOf(13))
        .comparisonSummary("An average master bedroom in Varthur measures 149 sq ft. This unit's master bedroom is 13% bigger.")
        .active(true)
        .sortOrder(1)
        .build();

    FloorPlanRoomDimensionResponse response = ProjectFloorPlanRoomDimensionMapper.toResponse(entity);

    assertThat(response.isHasComparisonData()).isTrue();
    assertThat(response.getComparisonLabel()).isEqualTo("+13% from avg");
    assertThat(response.getSummary())
        .isEqualTo("An average master bedroom in Varthur measures 149 sq ft. This unit's master bedroom is 13% bigger.");
    assertThat(response.getAverageAreaSqft()).isEqualByComparingTo("149");
  }

  @Test
  void roomWithBelowAverageDifferenceOmitsPlusSign() {
    ProjectFloorPlanRoomDimensionEntity entity = ProjectFloorPlanRoomDimensionEntity.builder()
        .roomType(FloorPlanRoomType.KITCHEN)
        .areaSqft(BigDecimal.valueOf(120))
        .averageAreaSqft(BigDecimal.valueOf(130))
        .differencePercent(BigDecimal.valueOf(-8))
        .comparisonSummary("Smaller than average.")
        .build();

    FloorPlanRoomDimensionResponse response = ProjectFloorPlanRoomDimensionMapper.toResponse(entity);

    assertThat(response.isHasComparisonData()).isTrue();
    assertThat(response.getComparisonLabel()).isEqualTo("-8% from avg");
  }

  @Test
  void roomWithoutAuthoredComparisonDataOmitsBadgeAndSummary() {
    ProjectFloorPlanRoomDimensionEntity entity = ProjectFloorPlanRoomDimensionEntity.builder()
        .roomType(FloorPlanRoomType.DINING_ROOM)
        .dimensionText("12 ft * 14 ft")
        // averageAreaSqft intentionally left null - not authored yet
        .comparisonSummary("This should never surface without an average.")
        .build();

    FloorPlanRoomDimensionResponse response = ProjectFloorPlanRoomDimensionMapper.toResponse(entity);

    assertThat(response.isHasComparisonData()).isFalse();
    assertThat(response.getComparisonLabel()).isNull();
    assertThat(response.getSummary()).isNull();
    assertThat(response.getDimensionText()).isEqualTo("12 ft * 14 ft");
  }
}
