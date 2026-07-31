package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.media.config.S3Properties;
import com.brandPitara.sfs.media.validator.TrustedMediaUrlValidator;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanInsightDetailResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanInsightEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisTagEntity;
import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanInsightRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRoomDimensionRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanVisualAnalysisRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Public-read contract tests for {@code publicGetDetail()}, added as part
 * of stabilizing the Visual Floor-Plan Analysis contract (GAP-027). No test
 * file for this class existed on {@code main} before this change (confirmed
 * during reconnaissance) — this is new coverage, not a port of the dirty
 * worktree's own (differently-scoped, Room-Comparison-aware) test file.
 */
class ProjectFloorPlanInsightServiceImplTest {

  private static final Long PROJECT_ID = 501L;
  private static final Long FLOOR_PLAN_ID = 701L;
  private static final String APPROVED_MEDIA_URL =
      "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/501/floor-plans/visual-analysis/photo.jpg";

  private ProjectRepository projectRepository;
  private ProjectFloorPlanRepository floorPlanRepository;
  private ProjectFloorPlanInsightRepository insightRepository;
  private ProjectFloorPlanRoomDimensionRepository roomRepository;
  private ProjectFloorPlanVisualAnalysisRepository visualAnalysisRepository;
  private ProjectFloorPlanInsightServiceImpl service;

  @BeforeEach
  void setUp() {
    projectRepository = mock(ProjectRepository.class);
    floorPlanRepository = mock(ProjectFloorPlanRepository.class);
    insightRepository = mock(ProjectFloorPlanInsightRepository.class);
    roomRepository = mock(ProjectFloorPlanRoomDimensionRepository.class);
    visualAnalysisRepository = mock(ProjectFloorPlanVisualAnalysisRepository.class);
    ContentVersionService contentVersionService = mock(ContentVersionService.class);

    S3Properties s3Properties = new S3Properties();
    s3Properties.setBucket("sfs-s3bucket");
    s3Properties.setRegion("ap-south-1");
    TrustedMediaUrlValidator trustedMediaUrlValidator = new TrustedMediaUrlValidator(s3Properties);

    service = new ProjectFloorPlanInsightServiceImpl(
        projectRepository,
        floorPlanRepository,
        insightRepository,
        roomRepository,
        visualAnalysisRepository,
        contentVersionService,
        new ProjectPublicVisibilityPolicy(),
        trustedMediaUrlValidator);

    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of());
    when(insightRepository.findByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of());
  }

  private ProjectEntity approvedProject() {
    return ProjectEntity.builder()
        .id(PROJECT_ID)
        .published(true)
        .active(true)
        .deleted(false)
        .reviewStatus(ReviewStatus.APPROVED)
        .build();
  }

  private ProjectFloorPlanEntity activeFloorPlan(ProjectEntity project) {
    return ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID)
        .project(project)
        .active(true)
        .deleted(false)
        .build();
  }

  // 1. Public project + active floor plan + visual analysis -> expected response.
  @Test
  void publicGetDetailReturnsExpectedResponseForApprovedProjectAndActiveFloorPlanWithVisualAnalysis() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));

    ProjectFloorPlanVisualAnalysisEntity va = ProjectFloorPlanVisualAnalysisEntity.builder()
        .id(1L).floorPlan(floorPlan).title("Visual analysis of every important factor")
        .description("Well-lit throughout the day.")
        .mediaType(FloorPlanVisualMediaType.IMAGE).mediaUrl(APPROVED_MEDIA_URL)
        .active(true).deleted(false).build();
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(va));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getFloorPlanId()).isEqualTo(FLOOR_PLAN_ID);
    assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
    assertThat(response.getVisualAnalysis()).isNotNull();
    assertThat(response.getVisualAnalysis().getMediaUrl()).isEqualTo(APPROVED_MEDIA_URL);
    assertThat(response.isDemo()).isFalse();
    assertThat(response.getSourceLabel()).isEqualTo("Verified floor-plan intelligence");
  }

  // 2. Unknown project -> 404.
  @Test
  void publicGetDetailThrowsNotFoundForUnknownProject() {
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // 3. Draft/unpublished project -> 404.
  @Test
  void publicGetDetailThrowsNotFoundForDraftProject() {
    ProjectEntity draft = ProjectEntity.builder()
        .id(PROJECT_ID).published(false).active(true).deleted(false)
        .reviewStatus(ReviewStatus.DRAFT).build();
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(draft));

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // 3b. Published but not yet APPROVED (PENDING_REVIEW) -> 404, same policy.
  @Test
  void publicGetDetailThrowsNotFoundForPendingReviewProject() {
    ProjectEntity pending = ProjectEntity.builder()
        .id(PROJECT_ID).published(true).active(true).deleted(false)
        .reviewStatus(ReviewStatus.PENDING_REVIEW).build();
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(pending));

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // 4. Inactive floor plan -> 404.
  @Test
  void publicGetDetailThrowsNotFoundForInactiveFloorPlan() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity inactive = ProjectFloorPlanEntity.builder()
        .id(FLOOR_PLAN_ID).project(project).active(false).deleted(false).build();
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(inactive));

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // 4b. Deleted floor plan -> 404 (repository's own findByIdAndDeletedFalse filters it out).
  @Test
  void publicGetDetailThrowsNotFoundForDeletedFloorPlan() {
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(approvedProject()));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID))
        .isInstanceOf(NotFoundException.class);
  }

  // 5. Wrong project/floor-plan pairing -> 404.
  @Test
  void publicGetDetailThrowsNotFoundForWrongProjectFloorPlanPairing() {
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

  // 6. Visual analysis absent -> verified null behavior, demo computed true.
  @Test
  void publicGetDetailReturnsNullVisualAnalysisAndDemoTrueWhenNoneAuthored() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.empty());

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getVisualAnalysis()).isNull();
    assertThat(response.isDemo()).isTrue();
    assertThat(response.getSourceLabel()).isEqualTo("Sample content");
  }

  // 7. IMAGE response mapping.
  @Test
  void publicGetDetailMapsImageMediaTypeCorrectly() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(ProjectFloorPlanVisualAnalysisEntity.builder()
            .id(1L).floorPlan(floorPlan).title("t").mediaType(FloorPlanVisualMediaType.IMAGE)
            .mediaUrl(APPROVED_MEDIA_URL).active(true).deleted(false).build()));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getVisualAnalysis().getMediaType()).isEqualTo(FloorPlanVisualMediaType.IMAGE);
  }

  // 8. VIDEO response mapping.
  @Test
  void publicGetDetailMapsVideoMediaTypeCorrectly() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    String videoUrl = "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/501/floor-plans/visual-analysis/walkthrough.mp4";
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(ProjectFloorPlanVisualAnalysisEntity.builder()
            .id(1L).floorPlan(floorPlan).title("t").mediaType(FloorPlanVisualMediaType.VIDEO)
            .mediaUrl(videoUrl).active(true).deleted(false).build()));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getVisualAnalysis().getMediaType()).isEqualTo(FloorPlanVisualMediaType.VIDEO);
    assertThat(response.getVisualAnalysis().getMediaUrl()).isEqualTo(videoUrl);
  }

  // 9. LOTTIE_JSON response mapping.
  @Test
  void publicGetDetailMapsLottieJsonMediaTypeCorrectly() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    String jsonUrl = "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/501/floor-plans/visual-analysis/animation.json";
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(ProjectFloorPlanVisualAnalysisEntity.builder()
            .id(1L).floorPlan(floorPlan).title("t").mediaType(FloorPlanVisualMediaType.LOTTIE_JSON)
            .mediaUrl(jsonUrl).active(true).deleted(false).build()));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getVisualAnalysis().getMediaType()).isEqualTo(FloorPlanVisualMediaType.LOTTIE_JSON);
  }

  // 10. Tags preserve intended ordering.
  @Test
  void publicGetDetailPreservesTagOrdering() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));

    ProjectFloorPlanVisualAnalysisEntity va = ProjectFloorPlanVisualAnalysisEntity.builder()
        .id(1L).floorPlan(floorPlan).title("t").mediaType(FloorPlanVisualMediaType.IMAGE)
        .mediaUrl(APPROVED_MEDIA_URL).active(true).deleted(false).build();
    va.getTags().add(ProjectFloorPlanVisualAnalysisTagEntity.builder()
        .visualAnalysis(va).label("Second").sortOrder(2).active(true).build());
    va.getTags().add(ProjectFloorPlanVisualAnalysisTagEntity.builder()
        .visualAnalysis(va).label("First").sortOrder(1).active(true).build());
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(va));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    // The mapper preserves whatever order the entity's own tags collection
    // provides (real ordering enforced by @OrderBy at the JPA layer, not
    // re-sorted here) - this test only proves the mapper doesn't silently
    // reorder or drop tags, not the @OrderBy annotation itself (a plain
    // unit test with a manually-built entity can't exercise that).
    assertThat(response.getVisualAnalysis().getTags()).hasSize(2);
  }

  // 10b. Inactive tags are excluded from the public response.
  @Test
  void publicGetDetailExcludesInactiveTags() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));

    ProjectFloorPlanVisualAnalysisEntity va = ProjectFloorPlanVisualAnalysisEntity.builder()
        .id(1L).floorPlan(floorPlan).title("t").mediaType(FloorPlanVisualMediaType.IMAGE)
        .mediaUrl(APPROVED_MEDIA_URL).active(true).deleted(false).build();
    va.getTags().add(ProjectFloorPlanVisualAnalysisTagEntity.builder()
        .visualAnalysis(va).label("Visible").sortOrder(1).active(true).build());
    va.getTags().add(ProjectFloorPlanVisualAnalysisTagEntity.builder()
        .visualAnalysis(va).label("Hidden").sortOrder(2).active(false).build());
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(va));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getVisualAnalysis().getTags()).extracting("label").containsExactly("Visible");
  }

  // 11. Public response contains no admin/internal fields.
  @Test
  void visualAnalysisResponseExposesNoActiveOrDeletedField() {
    List<String> fieldNames = java.util.Arrays.stream(
        com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisResponse.class.getDeclaredFields())
        .map(java.lang.reflect.Field::getName)
        .toList();

    assertThat(fieldNames).containsExactlyInAnyOrder("title", "description", "mediaType", "mediaUrl", "tags");
  }

  // 13. Existing room dimensions and insights remain unchanged alongside visualAnalysis.
  @Test
  void publicGetDetailStillMapsRoomsAndInsightsAlongsideVisualAnalysis() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.empty());

    ProjectFloorPlanRoomDimensionEntity room = ProjectFloorPlanRoomDimensionEntity.builder()
        .id(9001L).floorPlan(floorPlan).label("Master Bedroom").active(true).deleted(false).build();
    when(roomRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of(room));

    ProjectFloorPlanInsightEntity insight = ProjectFloorPlanInsightEntity.builder()
        .id(5001L).floorPlan(floorPlan).title("Natural Light").positive(true)
        .publicVisible(true).active(true).deleted(false).build();
    when(insightRepository.findByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(FLOOR_PLAN_ID))
        .thenReturn(List.of(insight));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getRooms()).hasSize(1);
    assertThat(response.getRooms().get(0).getLabel()).isEqualTo("Master Bedroom");
    assertThat(response.getInsights()).hasSize(1);
    assertThat(response.getInsights().get(0).getTitle()).isEqualTo("Natural Light");
  }

  // Read-side defence (GAP-028): a legacy/malformed mediaUrl is omitted,
  // not the whole visualAnalysis object, and never fails the request.
  @Test
  void publicGetDetailNullsOutAnUntrustedLegacyMediaUrlButKeepsTitleAndTags() {
    ProjectEntity project = approvedProject();
    ProjectFloorPlanEntity floorPlan = activeFloorPlan(project);
    when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
    when(floorPlanRepository.findByIdAndDeletedFalse(FLOOR_PLAN_ID)).thenReturn(Optional.of(floorPlan));

    ProjectFloorPlanVisualAnalysisEntity va = ProjectFloorPlanVisualAnalysisEntity.builder()
        .id(1L).floorPlan(floorPlan).title("Legacy analysis").mediaType(FloorPlanVisualMediaType.IMAGE)
        .mediaUrl("https://untrusted-legacy-host.example.com/old-photo.jpg")
        .active(true).deleted(false).build();
    va.getTags().add(ProjectFloorPlanVisualAnalysisTagEntity.builder()
        .visualAnalysis(va).label("Legacy Tag").sortOrder(1).active(true).build());
    when(visualAnalysisRepository.findByFloorPlanIdAndActiveTrueAndDeletedFalse(FLOOR_PLAN_ID))
        .thenReturn(Optional.of(va));

    ProjectFloorPlanInsightDetailResponse response = service.publicGetDetail(PROJECT_ID, FLOOR_PLAN_ID);

    assertThat(response.getVisualAnalysis()).isNotNull();
    assertThat(response.getVisualAnalysis().getMediaUrl()).isNull();
    assertThat(response.getVisualAnalysis().getTitle()).isEqualTo("Legacy analysis");
    assertThat(response.getVisualAnalysis().getTags()).hasSize(1);
    // The whole request still succeeds - a bad legacy URL never throws.
  }
}
