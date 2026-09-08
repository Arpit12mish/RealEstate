package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanGroupResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import com.brandPitara.sfs.project.service.ProjectMasterPlanService;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectDetailComposerImplTest {

  @Test
  void dashboardPreviewKeepsStructuredFloorPlanGroupingContract() {
    ProjectFloorPlanService floorPlans = mock(ProjectFloorPlanService.class);
    ProjectDetailComposerImpl composer = new ProjectDetailComposerImpl(
        floorPlans,
        mock(ProjectConnectivityService.class),
        mock(ProjectMeterService.class),
        mock(ProjectMasterPlanService.class)
    );
    when(floorPlans.dashboardPreviewList(42L)).thenReturn(List.of(
        floorPlan(1L, "3 BHK Type A", UnitConfigurationType.BHK_3),
        floorPlan(2L, "3 BHK Type B", UnitConfigurationType.BHK_3),
        floorPlan(3L, "3.5 BHK Type A", UnitConfigurationType.BHK_3_5)
    ));

    ProjectResponse response = composer.composeForPreview(project(), List.of(), null);

    assertThat(response.getFloorPlanGroups()).hasSize(2);
    assertGroup(response.getFloorPlanGroups().get(0), "BHK_3", "3 BHK", "3 BHK Type A", "3 BHK Type B");
    assertGroup(response.getFloorPlanGroups().get(1), "BHK_3_5", "3.5 BHK", "3.5 BHK Type A");
  }

  private ProjectEntity project() {
    return ProjectEntity.builder()
        .id(42L)
        .name("SFS Test Project")
        .active(true)
        .published(true)
        .deleted(false)
        .build();
  }

  private ProjectFloorPlanResponse floorPlan(Long id, String title, UnitConfigurationType type) {
    return ProjectFloorPlanResponse.builder()
        .id(id)
        .projectId(42L)
        .title(title)
        .unitConfigurationType(type)
        .unitConfigurationTypeLabel(type.toLabel())
        .active(true)
        .build();
  }

  private void assertGroup(ProjectFloorPlanGroupResponse group, String key, String label, String... titles) {
    assertThat(group.getGroupKey()).isEqualTo(key);
    assertThat(group.getGroupLabel()).isEqualTo(label);
    assertThat(group.getItems()).extracting(ProjectFloorPlanResponse::getTitle).containsExactly(titles);
  }
}
