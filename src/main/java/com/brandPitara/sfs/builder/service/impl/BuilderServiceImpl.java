package com.brandPitara.sfs.builder.service.impl;

import com.brandPitara.sfs.builder.dto.BuilderCardResponse;
import com.brandPitara.sfs.builder.dto.BuilderPublicResponse;
import com.brandPitara.sfs.builder.dto.BuilderResponse;
import com.brandPitara.sfs.builder.dto.BuilderUpsertRequest;
import com.brandPitara.sfs.builder.dto.UpdateBuilderLogoRequest;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.mapper.BuilderMapper;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.builder.service.BuilderService;
import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.media.validator.TrustedMediaUrlValidator;
import com.brandPitara.sfs.project.exception.PublicationConflictException;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BuilderServiceImpl implements BuilderService {

  private final BuilderRepository builderRepository;
  private final ContentVersionService contentVersionService;
  private final TrustedMediaUrlValidator trustedMediaUrlValidator;
  private final ProjectRepository projectRepository;
  private final ProjectPublicCacheEvictionPublisher cacheEvictionPublisher;

  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager em;

  private static final String KEY_BUILDERS = "BUILDERS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public BuilderResponse create(BuilderUpsertRequest request) {
    String name = clean(request.getName());
    String logoUrl = clean(request.getLogoUrl());
    trustedMediaUrlValidator.validate(logoUrl);
    BuilderEntity entity = BuilderEntity.builder()
        .name(name)
        .slug(generateUniqueSlug(name))
        .logoUrl(logoUrl)
        .description(clean(request.getDescription()))
        .phone(clean(request.getPhone()))
        .whatsapp(clean(request.getWhatsapp()))
        .email(clean(request.getEmail()))
        .addressLine(clean(request.getAddressLine()))
        .city(resolveCity(request.getCityId()))
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .priority(request.getPriority() != null ? request.getPriority() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .published(false)
        .deleted(false)
        .build();

    BuilderEntity saved = builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    return BuilderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public BuilderResponse update(Long id, BuilderUpsertRequest request) {
    boolean deactivating = Boolean.FALSE.equals(request.getActive());
    BuilderEntity entity = (deactivating
        ? builderRepository.findByIdAndDeletedFalseForUpdate(id)
        : builderRepository.findByIdAndDeletedFalse(id))
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    if (deactivating) {
      assertNoPublishedProjects(entity, "deactivated");
    }

    if (StringUtils.hasText(request.getName())) entity.setName(clean(request.getName()));
    if (request.getLogoUrl() != null) {
      String logoUrl = clean(request.getLogoUrl());
      trustedMediaUrlValidator.validate(logoUrl);
      entity.setLogoUrl(logoUrl);
    }
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));

    if (request.getPhone() != null) entity.setPhone(clean(request.getPhone()));
    if (request.getWhatsapp() != null) entity.setWhatsapp(clean(request.getWhatsapp()));
    if (request.getEmail() != null) entity.setEmail(clean(request.getEmail()));
    if (request.getAddressLine() != null) entity.setAddressLine(clean(request.getAddressLine()));

    if (request.getCityId() != null) entity.setCity(resolveCity(request.getCityId()));
    if (request.getLatitude() != null) entity.setLatitude(request.getLatitude());
    if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());

    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getActive() != null) entity.setActive(request.getActive());

    BuilderEntity saved = builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    if (Boolean.TRUE.equals(saved.getPublished()) && Boolean.TRUE.equals(saved.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
    evictAffectedProjects(id);
    return BuilderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public BuilderResponse updateLogo(Long id, UpdateBuilderLogoRequest request) {
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    String logoUrl = clean(request.getLogoUrl());
    trustedMediaUrlValidator.validate(logoUrl);
    entity.setLogoUrl(logoUrl);
    BuilderEntity saved = builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    if (Boolean.TRUE.equals(saved.getPublished()) && Boolean.TRUE.equals(saved.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
    evictAffectedProjects(id);
    return BuilderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public BuilderResponse setPublished(Long id, boolean published) {
    BuilderEntity entity = (published
        ? builderRepository.findByIdAndDeletedFalse(id)
        : builderRepository.findByIdAndDeletedFalseForUpdate(id))
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    if (!published) {
      assertNoPublishedProjects(entity, "unpublished");
    }

    entity.setPublished(published);
    BuilderEntity saved = builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    contentVersionService.bump(KEY_HOME);
    evictAffectedProjects(id);
    return BuilderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long id) {
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalseForUpdate(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    assertNoPublishedProjects(entity, "deleted");

    entity.setDeleted(true);
    entity.setPublished(false);
    builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    contentVersionService.bump(KEY_HOME);
    evictAffectedProjects(id);
  }

  @Override
  @Transactional(readOnly = true)
  public BuilderResponse getById(Long id) {
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));
    return BuilderMapper.toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BuilderResponse> adminList(Boolean published, Boolean active, Pageable pageable) {
    Page<BuilderEntity> page;

    if (published == null && active == null) {
      page = builderRepository.findByDeletedFalse(pageable);
    } else if (published != null && active == null) {
      page = builderRepository.findByPublishedAndDeletedFalse(published, pageable);
    } else if (published == null) {
      page = builderRepository.findByActiveAndDeletedFalse(active, pageable);
    } else {
      page = builderRepository.findByPublishedAndActiveAndDeletedFalse(published, active, pageable);
    }

    return page.map(BuilderMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BuilderPublicResponse> listPublished(Pageable pageable) {
    return builderRepository.findByPublishedTrueAndActiveTrueAndDeletedFalse(pageable)
        .map(BuilderMapper::toPublicResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BuilderPublicResponse publicGetById(Long id) {
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    if (!Boolean.TRUE.equals(entity.getPublished()) || !Boolean.TRUE.equals(entity.getActive())) {
      throw new EntityNotFoundException("Builder not found: " + id);
    }
    return BuilderMapper.toPublicResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public BuilderPublicResponse publicGetBySlug(String slug) {
    BuilderEntity entity = builderRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse(slug)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + slug));
    return BuilderMapper.toPublicResponse(entity);
  }

  // -------- helpers --------

  private String generateUniqueSlug(String name) {
    String base = slugify(name);
    String candidate = base;
    int suffix = 2;
    while (builderRepository.findBySlug(candidate).isPresent()) {
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String base = input == null ? "" : input.toLowerCase(java.util.Locale.ROOT).trim()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-+|-+$)", "");
    return base.isEmpty() ? "builder" : base;
  }

  private CityEntity resolveCity(Long cityId) {
    if (cityId == null) return null;
    // lightweight reference (no extra select unless needed)
    return em.getReference(CityEntity.class, cityId);
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

  private void evictAffectedProjects(Long builderId) {
    cacheEvictionPublisher.publishAll(
        projectRepository.findIdsByBuilderIdAndDeletedFalse(builderId),
        ProjectCacheEvictionReason.BUILDER_CHANGED
    );
  }

  private void assertNoPublishedProjects(BuilderEntity builder, String operation) {
    long publishedProjectCount = projectRepository
        .countByBuilderIdAndPublishedTrueAndDeletedFalse(builder.getId());
    if (publishedProjectCount == 0) {
      return;
    }

    String builderName = StringUtils.hasText(builder.getName())
        ? builder.getName()
        : "Builder " + builder.getId();
    String noun = publishedProjectCount == 1 ? "project depends" : "projects depend";
    throw new PublicationConflictException(
        "BUILDER_HAS_PUBLISHED_PROJECTS",
        builderName + " cannot be " + operation + " because " + publishedProjectCount
            + " published " + noun + " on it. Unpublish those projects first."
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<BuilderCardResponse> publicHomeBuilders(Long cityId, int limit) {

    int safeLimit = Math.min(Math.max(limit, 1), 20);

    List<BuilderEntity> builders = (cityId == null)
        ? builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc()
        : builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseAndCity_IdOrderByPriorityAscIdDesc(cityId);

    return builders.stream()
        .limit(safeLimit)
        .map(BuilderMapper::toCard)
        .toList();
  }

}
