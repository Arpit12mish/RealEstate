package com.brandPitara.sfs.project.policy;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProjectPublicVisibilityPolicyTest {

  private final ProjectPublicVisibilityPolicy policy = new ProjectPublicVisibilityPolicy();

  @Test
  void isPubliclyVisibleTrueWhenPublishedActiveNotDeletedAndApproved() {
    ProjectEntity entity = visibleProject();

    assertThat(policy.isPubliclyVisible(entity)).isTrue();
  }

  @Test
  void isPubliclyVisibleFalseForNullProject() {
    assertThat(policy.isPubliclyVisible(null)).isFalse();
  }

  @Test
  void isPubliclyVisibleFalseWhenNotPublished() {
    ProjectEntity entity = visibleProject();
    entity.setPublished(false);

    assertThat(policy.isPubliclyVisible(entity)).isFalse();
  }

  @Test
  void isPubliclyVisibleFalseWhenNotActive() {
    ProjectEntity entity = visibleProject();
    entity.setActive(false);

    assertThat(policy.isPubliclyVisible(entity)).isFalse();
  }

  @Test
  void isPubliclyVisibleFalseWhenDeleted() {
    ProjectEntity entity = visibleProject();
    entity.setDeleted(true);

    assertThat(policy.isPubliclyVisible(entity)).isFalse();
  }

  @Test
  void isPubliclyVisibleFalseWhenNotApproved() {
    ProjectEntity entity = visibleProject();
    entity.setReviewStatus(ReviewStatus.DRAFT);

    assertThat(policy.isPubliclyVisible(entity)).isFalse();
  }

  @Test
  void isPubliclyVisibleFalseWhenBuilderIsUnpublished() {
    ProjectEntity entity = visibleProject();
    entity.getBuilder().setPublished(false);

    assertThat(policy.isPubliclyVisible(entity)).isFalse();
  }

  @Test
  void publicationReportsInactiveBuilderWithStableCode() {
    ProjectEntity entity = visibleProject();
    entity.setPublished(false);
    entity.getBuilder().setActive(false);

    assertThatThrownBy(() -> policy.assertEligibleForPublication(entity, 51L))
        .isInstanceOf(com.brandPitara.sfs.project.exception.PublicationConflictException.class)
        .satisfies(ex -> assertThat(
            ((com.brandPitara.sfs.project.exception.PublicationConflictException) ex).getCode())
            .isEqualTo("PROJECT_BUILDER_INACTIVE"));
  }

  @Test
  void assertPubliclyVisibleByIdPassesSilentlyForVisibleProject() {
    assertThatCode(() -> policy.assertPubliclyVisible(visibleProject(), 51L))
        .doesNotThrowAnyException();
  }

  @Test
  void assertPubliclyVisibleByIdThrowsNotFoundWithIdInMessage() {
    ProjectEntity entity = visibleProject();
    entity.setPublished(false);

    assertThatThrownBy(() -> policy.assertPubliclyVisible(entity, 51L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("51");
  }

  // --- GAP-001: the slug-keyed overload ---

  @Test
  void assertPubliclyVisibleBySlugPassesSilentlyForVisibleProject() {
    assertThatCode(() -> policy.assertPubliclyVisible(visibleProject(), "m3m-antalya-hills"))
        .doesNotThrowAnyException();
  }

  @Test
  void assertPubliclyVisibleBySlugThrowsNotFoundWithSlugInMessageNotNull() {
    ProjectEntity entity = visibleProject();
    entity.setReviewStatus(ReviewStatus.DRAFT);

    assertThatThrownBy(() -> policy.assertPubliclyVisible(entity, "m3m-antalya-hills"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("m3m-antalya-hills");
  }

  private ProjectEntity visibleProject() {
    ProjectEntity entity = new ProjectEntity();
    entity.setId(51L);
    entity.setSlug("m3m-antalya-hills");
    entity.setPublished(true);
    entity.setActive(true);
    entity.setDeleted(false);
    entity.setReviewStatus(ReviewStatus.APPROVED);
    entity.setBuilder(BuilderEntity.builder()
        .id(7L)
        .name("Max Estates")
        .published(true)
        .active(true)
        .deleted(false)
        .build());
    return entity;
  }
}
