package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMasterPlanMapperTest {

  @Test
  void publicResponseIncludesOnlyUsableNonNullStats() {
    ProjectMasterPlanEntity entity = ProjectMasterPlanEntity.builder()
        .active(true)
        .deleted(false)
        .masterPlanImageUrl("https://cdn.example.com/master.webp")
        .totalUnits(1520)
        .parkAreaValue(new BigDecimal("2.70"))
        .parkAreaUnit(MasterPlanAreaUnit.ACRE)
        .waterSource("BWSSB")
        .remarks("internal note")
        .build();

    ProjectMasterPlanResponse response = ProjectMasterPlanMapper.toPublicResponse(entity);

    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isEqualTo("Master Plan");
    assertThat(response.getSubtitle()).isEqualTo("Site layout, towers & open spaces");
    assertThat(response.getExpandable()).isTrue();
    assertThat(response.getRemarks()).isNull();
    assertThat(response.getSourceDocumentUrl()).isNull();
    assertThat(response.getActive()).isNull();
    assertThat(response.getStats()).extracting("key")
        .containsExactly("TOTAL_UNITS", "PARK_AREA", "WATER_SOURCE");
    assertThat(response.getStats().get(1).getValue()).isEqualTo("2.7 Acres");
    assertThat(response.getStats().get(1).getUnit()).isEqualTo("ACRE");
  }

  @Test
  void publicResponseReturnsNullWhenInactiveOrEmpty() {
    ProjectMasterPlanEntity inactive = ProjectMasterPlanEntity.builder()
        .active(false)
        .deleted(false)
        .masterPlanImageUrl("https://cdn.example.com/master.webp")
        .build();

    ProjectMasterPlanEntity empty = ProjectMasterPlanEntity.builder()
        .active(true)
        .deleted(false)
        .build();

    assertThat(ProjectMasterPlanMapper.toPublicResponse(inactive)).isNull();
    assertThat(ProjectMasterPlanMapper.toPublicResponse(empty)).isNull();
  }
}
