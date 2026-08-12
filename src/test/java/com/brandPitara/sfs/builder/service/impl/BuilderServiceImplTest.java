package com.brandPitara.sfs.builder.service.impl;

import com.brandPitara.sfs.builder.dto.BuilderPublicResponse;
import com.brandPitara.sfs.builder.dto.BuilderResponse;
import com.brandPitara.sfs.builder.dto.BuilderUpsertRequest;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.media.validator.TrustedMediaUrlValidator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuilderServiceImplTest {

  @Mock private BuilderRepository builderRepository;
  @Mock private ContentVersionService contentVersionService;
  @Mock private TrustedMediaUrlValidator trustedMediaUrlValidator;

  @InjectMocks private BuilderServiceImpl builderService;

  private BuilderEntity existing(Long id, String name, String slug) {
    return BuilderEntity.builder()
        .id(id).name(name).slug(slug)
        .active(true).published(true).deleted(false).priority(0)
        .build();
  }

  // ---------- create(): slug generation ----------

  @Test
  void create_generatesASlugifiedSlugFromName_whenNoCollision() {
    when(builderRepository.findBySlug(anyString())).thenReturn(Optional.empty());
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> {
      BuilderEntity e = inv.getArgument(0);
      e.setId(1L);
      return e;
    });

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().name("Meridian Constructions").build();
    builderService.create(request);

    ArgumentCaptor<BuilderEntity> captor = ArgumentCaptor.forClass(BuilderEntity.class);
    verify(builderRepository).save(captor.capture());
    assertThat(captor.getValue().getSlug()).isEqualTo("meridian-constructions");
  }

  @Test
  void create_appendsNumericSuffix_whenBaseSlugAlreadyTaken() {
    when(builderRepository.findBySlug("meridian-constructions")).thenReturn(Optional.of(existing(1L, "x", "meridian-constructions")));
    when(builderRepository.findBySlug("meridian-constructions-2")).thenReturn(Optional.empty());
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().name("Meridian Constructions").build();
    builderService.create(request);

    ArgumentCaptor<BuilderEntity> captor = ArgumentCaptor.forClass(BuilderEntity.class);
    verify(builderRepository).save(captor.capture());
    assertThat(captor.getValue().getSlug()).isEqualTo("meridian-constructions-2");
  }

  @Test
  void create_keepsIncrementingSuffix_untilAnUnusedSlugIsFound() {
    when(builderRepository.findBySlug("m3m-india")).thenReturn(Optional.of(existing(1L, "x", "m3m-india")));
    when(builderRepository.findBySlug("m3m-india-2")).thenReturn(Optional.of(existing(2L, "x", "m3m-india-2")));
    when(builderRepository.findBySlug("m3m-india-3")).thenReturn(Optional.empty());
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().name("M3M India").build();
    builderService.create(request);

    ArgumentCaptor<BuilderEntity> captor = ArgumentCaptor.forClass(BuilderEntity.class);
    verify(builderRepository).save(captor.capture());
    assertThat(captor.getValue().getSlug()).isEqualTo("m3m-india-3");
  }

  @Test
  void create_normalizesPunctuationAndCase_toTheSameCanonicalForm() {
    when(builderRepository.findBySlug(anyString())).thenReturn(Optional.empty());
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().name("  M3M--India!! ").build();
    builderService.create(request);

    ArgumentCaptor<BuilderEntity> captor = ArgumentCaptor.forClass(BuilderEntity.class);
    verify(builderRepository).save(captor.capture());
    assertThat(captor.getValue().getSlug()).isEqualTo("m3m-india");
  }

  // ---------- update(): slug stability ----------

  @Test
  void update_preservesSlug_whenOnlyNameChanges() {
    BuilderEntity entity = existing(1L, "Old Name", "old-name-original-slug");
    when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().name("Brand New Name").build();
    builderService.update(1L, request);

    // The slug must not be regenerated from the new name - it stays exactly as it was.
    assertThat(entity.getSlug()).isEqualTo("old-name-original-slug");
    verify(builderRepository, never()).findBySlug(anyString());
  }

  @Test
  void update_preservesSlug_whenUnrelatedFieldsChange() {
    BuilderEntity entity = existing(1L, "Meridian Constructions", "meridian-constructions");
    when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder()
        .description("Updated description")
        .addressLine("New address")
        .priority(5)
        .build();
    builderService.update(1L, request);

    assertThat(entity.getSlug()).isEqualTo("meridian-constructions");
  }

  // ---------- MEDIA-H1: logoUrl write-path validation ----------

  @Test
  void create_validatesLogoUrlThroughTrustedMediaUrlValidator_beforeSaving() {
    when(builderRepository.findBySlug(anyString())).thenReturn(Optional.empty());
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder()
        .name("Meridian Constructions")
        .logoUrl("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/builders/1/logo.png")
        .build();
    builderService.create(request);

    verify(trustedMediaUrlValidator).validate("https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/builders/1/logo.png");
  }

  @Test
  void create_propagatesRejectionFromTrustedMediaUrlValidator_andNeverSaves() {
    org.mockito.Mockito.doThrow(new IllegalArgumentException("mediaUrl host is not on the approved media host allowlist"))
        .when(trustedMediaUrlValidator).validate("https://architectureideas.info/logo.jpg");

    BuilderUpsertRequest request = BuilderUpsertRequest.builder()
        .name("Untrusted Co")
        .logoUrl("https://architectureideas.info/logo.jpg")
        .build();

    assertThatThrownBy(() -> builderService.create(request)).isInstanceOf(IllegalArgumentException.class);
    verify(builderRepository, never()).save(any(BuilderEntity.class));
  }

  @Test
  void create_invokesValidatorWithNull_whenLogoUrlIsAbsent_validatorAcceptsNullAsOptional() {
    when(builderRepository.findBySlug(anyString())).thenReturn(Optional.empty());
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().name("No Logo Co").build();
    builderService.create(request);

    verify(trustedMediaUrlValidator).validate(null);
  }

  @Test
  void update_validatesLogoUrl_onlyWhenTheRequestActuallyChangesIt() {
    BuilderEntity entity = existing(1L, "Meridian Constructions", "meridian-constructions");
    when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().description("Updated description").build();
    builderService.update(1L, request);

    verify(trustedMediaUrlValidator, never()).validate(anyString());
  }

  @Test
  void update_ofAnUnrelatedField_neverRevalidatesOrTouchesAnAlreadyPersistedLegacyExternalLogoUrl() {
    // A pre-existing row already carries a legacy, unvalidated external logo
    // URL (the exact real-world MEDIA-H1 shape: written before this
    // validator existed). An editor changing something unrelated (e.g.
    // description) must still succeed — the legacy value must not be
    // revalidated or rejected retroactively, and must survive untouched.
    BuilderEntity entity = BuilderEntity.builder()
        .id(1L).name("Tata Housing").slug("tata-housing")
        .logoUrl("https://architectureideas.info/wp-content/uploads/2014/09/Tata-Housing1.jpg")
        .active(true).published(true).deleted(false).priority(0)
        .build();
    when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
    when(builderRepository.save(any(BuilderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().description("Corrected description").build();
    BuilderResponse response = builderService.update(1L, request);

    verify(trustedMediaUrlValidator, never()).validate(anyString());
    verify(builderRepository).save(any(BuilderEntity.class));
    assertThat(entity.getLogoUrl()).isEqualTo("https://architectureideas.info/wp-content/uploads/2014/09/Tata-Housing1.jpg");
    assertThat(response).isNotNull();
  }

  @Test
  void update_propagatesRejectionFromTrustedMediaUrlValidator_andNeverPersistsTheBadUrl() {
    BuilderEntity entity = existing(1L, "Meridian Constructions", "meridian-constructions");
    when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
    org.mockito.Mockito.doThrow(new IllegalArgumentException("mediaUrl host is not on the approved media host allowlist"))
        .when(trustedMediaUrlValidator).validate("https://architectureideas.info/logo.jpg");

    BuilderUpsertRequest request = BuilderUpsertRequest.builder().logoUrl("https://architectureideas.info/logo.jpg").build();

    assertThatThrownBy(() -> builderService.update(1L, request)).isInstanceOf(IllegalArgumentException.class);
    verify(builderRepository, never()).save(any(BuilderEntity.class));
  }

  @Test
  void updateLogo_validatesThroughTrustedMediaUrlValidator_beforePersisting() {
    BuilderEntity entity = existing(1L, "Meridian Constructions", "meridian-constructions");
    when(builderRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(entity));
    org.mockito.Mockito.doThrow(new IllegalArgumentException("mediaUrl host is not on the approved media host allowlist"))
        .when(trustedMediaUrlValidator).validate("https://architectureideas.info/logo.jpg");

    com.brandPitara.sfs.builder.dto.UpdateBuilderLogoRequest request =
        com.brandPitara.sfs.builder.dto.UpdateBuilderLogoRequest.builder()
            .logoUrl("https://architectureideas.info/logo.jpg")
            .build();

    assertThatThrownBy(() -> builderService.updateLogo(1L, request)).isInstanceOf(IllegalArgumentException.class);
    verify(builderRepository, never()).save(any(BuilderEntity.class));
  }

  // ---------- publicGetBySlug(): visibility enforcement ----------

  @Test
  void publicGetBySlug_returnsMappedResponse_forPublicVisibleBuilder() {
    BuilderEntity entity = existing(1L, "Meridian Constructions", "meridian-constructions");
    when(builderRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("meridian-constructions"))
        .thenReturn(Optional.of(entity));

    BuilderPublicResponse response = builderService.publicGetBySlug("meridian-constructions");

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getSlug()).isEqualTo("meridian-constructions");
    assertThat(response.getName()).isEqualTo("Meridian Constructions");
  }

  @Test
  void publicGetBySlug_throwsNotFound_forUnknownSlug() {
    when(builderRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("does-not-exist"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> builderService.publicGetBySlug("does-not-exist"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void publicGetBySlug_throwsNotFound_whenRepositoryVisibilityQueryExcludesTheRow() {
    // The repository method itself enforces published/active/deleted - an inactive,
    // deleted, or unpublished builder's slug is indistinguishable from an unknown one
    // at this layer (query-level visibility, not controller-level filtering).
    when(builderRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("hidden-builder"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> builderService.publicGetBySlug("hidden-builder"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  // ---------- listPublished(): slug present on every list item ----------

  @Test
  void listPublished_mapsEveryItemThroughToPublicResponse_includingSlug() {
    BuilderEntity entity = existing(1L, "Meridian Constructions", "meridian-constructions");
    org.springframework.data.domain.Page<BuilderEntity> page =
        new org.springframework.data.domain.PageImpl<>(java.util.List.of(entity));
    when(builderRepository.findByPublishedTrueAndActiveTrueAndDeletedFalse(any())).thenReturn(page);

    var result = builderService.listPublished(org.springframework.data.domain.PageRequest.of(0, 20));

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getSlug()).isEqualTo("meridian-constructions");
  }
}
