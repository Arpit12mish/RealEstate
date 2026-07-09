package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanGroupResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import com.brandPitara.sfs.project.service.ProjectMasterPlanService;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDetailComposerImplTest {

  private static final Long PROJECT_ID = 42L;

  @Mock private ProjectFloorPlanService floorPlanService;
  @Mock private ProjectConnectivityService connectivityService;
  @Mock private ProjectMeterService meterService;
  @Mock private ProjectMasterPlanService masterPlanService;

  @Test
  void groupsStructuredBhk3FloorPlansWithReadableLabel() {
    ProjectDetailComposerImpl composer = composer();
    when(floorPlanService.publicList(PROJECT_ID))
        .thenReturn(List.of(floorPlan(1L, "3 BHK Type A", UnitConfigurationType.BHK_3)));

    ProjectPublicResponse response = composer.composePublic(project(), List.of());

    assertThat(response.getFloorPlanGroups())
        .singleElement()
        .satisfies(group -> {
          assertThat(group.getGroupKey()).isEqualTo("BHK_3");
          assertThat(group.getGroupLabel()).isEqualTo("3 BHK");
          assertThat(group.getItems()).extracting(ProjectFloorPlanResponse::getTitle)
              .containsExactly("3 BHK Type A");
        });
  }

  @Test
  void groupsStructuredDecimalBhkFloorPlansWithReadableLabels() {
    ProjectDetailComposerImpl composer = composer();
    when(floorPlanService.publicList(PROJECT_ID))
        .thenReturn(List.of(
            floorPlan(1L, "3.5 BHK Type A", UnitConfigurationType.BHK_3_5),
            floorPlan(2L, "4.5 BHK Type A", UnitConfigurationType.BHK_4_5)
        ));

    ProjectPublicResponse response = composer.composePublic(project(), List.of());

    assertThat(response.getFloorPlanGroups()).hasSize(2);
    assertGroup(response.getFloorPlanGroups().get(0), "BHK_3_5", "3.5 BHK", "3.5 BHK Type A");
    assertGroup(response.getFloorPlanGroups().get(1), "BHK_4_5", "4.5 BHK", "4.5 BHK Type A");
  }

  @Test
  void keepsMultipleVariantsUnderSameStructuredConfiguration() {
    ProjectDetailComposerImpl composer = composer();
    when(floorPlanService.publicList(PROJECT_ID))
        .thenReturn(List.of(
            floorPlan(1L, "3 BHK Type A", UnitConfigurationType.BHK_3),
            floorPlan(2L, "3 BHK Type B", UnitConfigurationType.BHK_3)
        ));

    ProjectPublicResponse response = composer.composePublic(project(), List.of());

    assertThat(response.getFloorPlanGroups())
        .singleElement()
        .satisfies(group -> {
          assertThat(group.getGroupKey()).isEqualTo("BHK_3");
          assertThat(group.getGroupLabel()).isEqualTo("3 BHK");
          assertThat(group.getItems()).extracting(ProjectFloorPlanResponse::getTitle)
              .containsExactly("3 BHK Type A", "3 BHK Type B");
        });
  }

  @Test
  void structuredConfigurationControlsGroupEvenWhenTitleMentionsDifferentBhk() {
    ProjectDetailComposerImpl composer = composer();
    when(floorPlanService.publicList(PROJECT_ID))
        .thenReturn(List.of(floorPlan(1L, "2 BHK Marketing Title", UnitConfigurationType.BHK_3)));

    ProjectPublicResponse response = composer.composePublic(project(), List.of());

    assertThat(response.getFloorPlanGroups())
        .singleElement()
        .satisfies(group -> {
          assertThat(group.getGroupKey()).isEqualTo("BHK_3");
          assertThat(group.getGroupLabel()).isEqualTo("3 BHK");
          assertThat(group.getItems()).extracting(ProjectFloorPlanResponse::getTitle)
              .containsExactly("2 BHK Marketing Title");
        });
  }

  @Test
  void fallbackParserStillGroupsOldRecordsWithoutStructuredConfiguration() {
    ProjectDetailComposerImpl composer = composer();
    when(floorPlanService.publicList(PROJECT_ID))
        .thenReturn(List.of(legacyFloorPlan(1L, "2 BHK Type A")));

    ProjectPublicResponse response = composer.composePublic(project(), List.of());

    assertThat(response.getFloorPlanGroups())
        .singleElement()
        .satisfies(group -> assertGroup(group, "BHK_2", "2 BHK", "2 BHK Type A"));
  }

  @Test
  void fallbackParserSupportsDecimalAndFivePlusBhkOldRecords() {
    ProjectDetailComposerImpl composer = composer();
    when(floorPlanService.publicList(PROJECT_ID))
        .thenReturn(List.of(
            legacyFloorPlan(1L, "3.5 BHK Type A"),
            legacyFloorPlan(2L, "4.5 BHK Type A"),
            legacyFloorPlan(3L, "5+ BHK Penthouse")
        ));

    ProjectPublicResponse response = composer.composePublic(project(), List.of());

    assertThat(response.getFloorPlanGroups()).hasSize(3);
    assertGroup(response.getFloorPlanGroups().get(0), "BHK_3_5", "3.5 BHK", "3.5 BHK Type A");
    assertGroup(response.getFloorPlanGroups().get(1), "BHK_4_5", "4.5 BHK", "4.5 BHK Type A");
    assertGroup(response.getFloorPlanGroups().get(2), "BHK_5_PLUS", "5+ BHK", "5+ BHK Penthouse");
  }

  @Test
  void dashboardPreviewUsesSameStructuredFloorPlanGroups() {
    ProjectDetailComposerImpl composer = composer();
    when(floorPlanService.dashboardPreviewList(PROJECT_ID))
        .thenReturn(List.of(
            floorPlan(1L, "3 BHK Type A", UnitConfigurationType.BHK_3),
            floorPlan(2L, "3 BHK Type B", UnitConfigurationType.BHK_3),
            floorPlan(3L, "3.5 BHK Type A", UnitConfigurationType.BHK_3_5)
        ));

    ProjectResponse response = composer.composeForPreview(project(), List.of(), null);

    assertThat(response.getFloorPlanGroups()).hasSize(2);
    assertGroup(response.getFloorPlanGroups().get(0), "BHK_3", "3 BHK", "3 BHK Type A", "3 BHK Type B");
    assertGroup(response.getFloorPlanGroups().get(1), "BHK_3_5", "3.5 BHK", "3.5 BHK Type A");
  }

  private ProjectDetailComposerImpl composer() {
    return new ProjectDetailComposerImpl(floorPlanService, connectivityService, meterService, masterPlanService);
  }

  private ProjectEntity project() {
    return ProjectEntity.builder()
        .id(PROJECT_ID)
        .name("SFS Test Project")
        .active(true)
        .published(true)
        .deleted(false)
        .build();
  }

  private ProjectFloorPlanResponse floorPlan(Long id, String title, UnitConfigurationType unitConfigurationType) {
    return ProjectFloorPlanResponse.builder()
        .id(id)
        .projectId(PROJECT_ID)
        .title(title)
        .imageUrl("https://cdn.example.com/floorplans/" + id + ".webp")
        .unitConfigurationType(unitConfigurationType)
        .unitConfigurationTypeLabel(unitConfigurationType.toLabel())
        .active(true)
        .build();
  }

  private ProjectFloorPlanResponse legacyFloorPlan(Long id, String title) {
    return ProjectFloorPlanResponse.builder()
        .id(id)
        .projectId(PROJECT_ID)
        .title(title)
        .imageUrl("https://cdn.example.com/floorplans/" + id + ".webp")
        .active(true)
        .build();
  }

  private void assertGroup(ProjectFloorPlanGroupResponse group, String key, String label, String... titles) {
    assertThat(group.getGroupKey()).isEqualTo(key);
    assertThat(group.getGroupLabel()).isEqualTo(label);
    assertThat(group.getItems()).extracting(ProjectFloorPlanResponse::getTitle)
        .containsExactly(titles);
  }
}
