package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.media.validator.TrustedMediaUrlValidator;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisUpsertRequest;
import com.brandPitara.sfs.project.dto.VisualAnalysisTagUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisTagEntity;
import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanVisualAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectFloorPlanVisualAnalysisServiceImplTest {

  private ProjectFloorPlanRepository floorPlanRepository;
  private ProjectFloorPlanVisualAnalysisRepository visualAnalysisRepository;
  private ContentVersionService contentVersionService;
  private ProjectFloorPlanVisualAnalysisServiceImpl service;

  private static final Long PROJECT_ID = 123L;
  private static final Long FLOOR_PLAN_ID = 991L;

  // A real TrustedMediaUrlValidator (not mocked) — exercises real GAP-028
  // validation logic through this service's own upsert() path, using the
  // same bucket/region test doubles TrustedMediaUrlValidatorTest itself
  // uses. Exhaustive positive/negative validator coverage lives in that
  // dedicated test file; this one only needs approved-host URLs to pass
  // through undisturbed.
  @BeforeEach
  void setUp() {
    floorPlanRepository = mock(ProjectFloorPlanRepository.class);
    visualAnalysisRepository = mock(ProjectFloorPlanVisualAnalysisRepository.class);
    contentVersionService = mock(ContentVersionService.class);
    S3Properties s3Properties = new S3Properties();
    s3Properties.setBucket("sfs-s3bucket");
    s3Properties.setRegion("ap-south-1");
    TrustedMediaUrlValidator trustedMediaUrlValidator = new TrustedMediaUrlValidator(s3Properties);
    service = new ProjectFloorPlanVisualAnalysisServiceImpl(
        floorPlanRepository, visualAnalysisRepository, contentVersionService, trustedMediaUrlValidator);

    ProjectFloorPlanEntity floorPlan = ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID)
        .project(ProjectEntity.builder().id(PROJECT_ID).build())
        .build();
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
  }

  @Test
  void upsertCreatesRowWithDefaultsWhenNoneExistsYet() {
    when(visualAnalysisRepository.findByFloorPlanIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.empty());
    when(visualAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectFloorPlanVisualAnalysisUpsertRequest request = ProjectFloorPlanVisualAnalysisUpsertRequest.builder()
        .description("Visualize how natural light, ventilation, and Vastu influence your space.")
        .mediaType(FloorPlanVisualMediaType.VIDEO)
        .mediaUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/123/floor-plans/visual-analysis/insight.mp4")
        .tags(List.of(
            VisualAnalysisTagUpsertRequest.builder().label("Sun Lighting").color("#3B7DDD").sortOrder(1).build(),
            VisualAnalysisTagUpsertRequest.builder().label("Air ventilation").sortOrder(2).build()
        ))
        .build();

    ProjectFloorPlanVisualAnalysisResponse response = service.upsert(PROJECT_ID, FLOOR_PLAN_ID, request);

    assertThat(response.getTitle()).isEqualTo("Visual analysis of every important factor");
    assertThat(response.getMediaType()).isEqualTo(FloorPlanVisualMediaType.VIDEO);
    assertThat(response.getMediaUrl()).isEqualTo("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/123/floor-plans/visual-analysis/insight.mp4");
    assertThat(response.getTags()).hasSize(2);
    assertThat(response.getTags().get(0).getLabel()).isEqualTo("Sun Lighting");
    assertThat(response.getTags().get(1).getColor()).isEqualTo("#3B7DDD");
    verify(contentVersionService).bump("PROJECTS");
  }

  @Test
  void upsertOnExistingRowUpdatesOnlyProvidedFieldsAndReplacesTags() {
    ProjectFloorPlanVisualAnalysisEntity existing = ProjectFloorPlanVisualAnalysisEntity.builder()
        .id(5L)
        .floorPlan(ProjectFloorPlanEntity.builder().id(FLOOR_PLAN_ID).build())
        .title("Existing title")
        .mediaType(FloorPlanVisualMediaType.IMAGE)
        .mediaUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/123/floor-plans/visual-analysis/old.webp")
        .build();
    existing.getTags().add(ProjectFloorPlanVisualAnalysisTagEntity.builder()
        .visualAnalysis(existing).label("Old Tag").color("#000000").sortOrder(0).build());

    when(visualAnalysisRepository.findByFloorPlanIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(existing));
    when(visualAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectFloorPlanVisualAnalysisUpsertRequest request = ProjectFloorPlanVisualAnalysisUpsertRequest.builder()
        .mediaUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/123/floor-plans/visual-analysis/new.webp")
        .tags(List.of(VisualAnalysisTagUpsertRequest.builder().label("Vastu analysis").sortOrder(1).build()))
        .build();

    ProjectFloorPlanVisualAnalysisResponse response = service.upsert(PROJECT_ID, FLOOR_PLAN_ID, request);

    assertThat(response.getTitle()).isEqualTo("Existing title");
    assertThat(response.getMediaUrl()).isEqualTo("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/123/floor-plans/visual-analysis/new.webp");
    assertThat(response.getTags()).extracting("label").containsExactly("Vastu analysis");
  }

  @Test
  void getReturnsNullWhenNoneAuthoredYet() {
    when(visualAnalysisRepository.findByFloorPlanIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.empty());

    assertThat(service.get(PROJECT_ID, FLOOR_PLAN_ID)).isNull();
  }

  @Test
  void upsertRejectsFloorPlanBelongingToAnotherProject() {
    assertThatThrownBy(() -> service.upsert(999L, FLOOR_PLAN_ID, ProjectFloorPlanVisualAnalysisUpsertRequest.builder().build()))
        .isInstanceOf(NotFoundException.class);
  }
}
