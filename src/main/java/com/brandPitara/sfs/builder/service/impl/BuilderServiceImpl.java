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
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.CityEntity;
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

  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager em;

  private static final String KEY_BUILDERS = "BUILDERS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public BuilderResponse create(BuilderUpsertRequest request) {
    String name = clean(request.getName());
    BuilderEntity entity = BuilderEntity.builder()
        .name(name)
        .slug(generateUniqueSlug(name))
        .logoUrl(clean(request.getLogoUrl()))
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
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    if (StringUtils.hasText(request.getName())) entity.setName(clean(request.getName()));
    if (request.getLogoUrl() != null) entity.setLogoUrl(clean(request.getLogoUrl()));
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
    return BuilderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public BuilderResponse updateLogo(Long id, UpdateBuilderLogoRequest request) {
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    entity.setLogoUrl(clean(request.getLogoUrl()));
    BuilderEntity saved = builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    if (Boolean.TRUE.equals(saved.getPublished()) && Boolean.TRUE.equals(saved.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
    return BuilderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public BuilderResponse setPublished(Long id, boolean published) {
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    entity.setPublished(published);
    BuilderEntity saved = builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    contentVersionService.bump(KEY_HOME);
    return BuilderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long id) {
    BuilderEntity entity = builderRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + id));

    entity.setDeleted(true);
    entity.setPublished(false);
    builderRepository.save(entity);

    contentVersionService.bump(KEY_BUILDERS);
    contentVersionService.bump(KEY_HOME);
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
