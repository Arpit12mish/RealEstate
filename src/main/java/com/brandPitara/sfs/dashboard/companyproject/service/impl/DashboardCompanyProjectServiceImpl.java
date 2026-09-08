package com.brandPitara.sfs.dashboard.companyproject.service.impl;

import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.companyproject.dto.*;
import com.brandPitara.sfs.dashboard.companyproject.service.DashboardCompanyProjectService;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardCompanyProjectServiceImpl implements DashboardCompanyProjectService {

  private final CompanyProjectRepository companyProjectRepository;
  private final CompanyRepository companyRepository;
  private final CityRepository cityRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<CompanyProjectListItemResponse> list(
      String q,
      Long companyId,
      String companyType,
      Long cityId,
      Boolean active,
      Pageable pageable
  ) {
    Page<CompanyProjectEntity> page = StringUtils.hasText(q)
        ? companyProjectRepository.searchForDashboardByName(q.trim(), companyId, companyType, cityId, active, pageable)
        : companyProjectRepository.searchForDashboard(companyId, companyType, cityId, active, pageable);

    List<Long> ids = page.getContent().stream().map(CompanyProjectEntity::getId).toList();
    Map<Long, Long> brandsUsedCountById = batchBrandsUsedCounts(ids);

    return page.map(cp -> toListItem(cp, brandsUsedCountById.getOrDefault(cp.getId(), 0L)));
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyProjectDetailResponse getDetail(Long companyProjectId) {
    CompanyProjectEntity p = companyProjectRepository.findByIdAndDeletedFalse(companyProjectId)
        .orElseThrow(() -> new EntityNotFoundException("Company project not found: " + companyProjectId));

    return toDetail(p);
  }

  @Override
  @Transactional
  public CompanyProjectDetailResponse create(CompanyProjectCreateRequest request) {
    CompanyEntity company = resolveCompany(request.getCompanyId());
    CompanyProjectEntity entity = CompanyProjectEntity.builder()
        .company(company)
        .name(request.getName().trim())
        .slug(resolveSlugForCreate(clean(request.getSlug()), request.getName()))
        .shortDescription(clean(request.getShortDescription()))
        .description(clean(request.getDescription()))
        .city(resolveCity(request.getCityId()))
        .addressLine(clean(request.getAddressLine()))
        .locationLabel(clean(request.getLocationLabel()))
        .clientName(clean(request.getClientName()))
        .projectArea(clean(request.getProjectArea()))
        .detail3(clean(request.getDetail3()))
        .tags(joinTags(request.getTags()))
        .coverMediaUrl(clean(request.getCoverMediaUrl()))
        .coverMediaType(defaultMediaType(request.getCoverMediaType(), request.getCoverMediaUrl()))
        .stats(request.getStats() != null ? request.getStats() : List.of())
        .budget(request.getBudget() != null ? request.getBudget() : new com.brandPitara.sfs.company.dto.CompanyProjectBudgetDto())
        .priceBreakdown(request.getPriceBreakdown() != null ? request.getPriceBreakdown() : List.of())
        .clientRequirements(request.getClientRequirements() != null ? request.getClientRequirements() : List.of())
        .designMaterials(request.getDesignMaterials() != null ? request.getDesignMaterials() : new com.brandPitara.sfs.company.dto.CompanyProjectDesignMaterialsDto())
        .active(request.getActive() != null ? request.getActive() : true)
        .published(request.getPublished() != null ? request.getPublished() : false)
        .priority(request.getPriority() != null ? request.getPriority() : 0)
        .deleted(false)
        .build();
    return toDetail(companyProjectRepository.save(entity));
  }

  @Override
  @Transactional
  public CompanyProjectDetailResponse update(Long companyProjectId, CompanyProjectUpdateRequest request) {
    CompanyProjectEntity entity = companyProjectRepository.findByIdAndDeletedFalse(companyProjectId)
        .orElseThrow(() -> new EntityNotFoundException("Company project not found: " + companyProjectId));
    if (request.getCompanyId() != null) entity.setCompany(resolveCompany(request.getCompanyId()));
    if (request.getName() != null) {
      if (!StringUtils.hasText(request.getName())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
      entity.setName(request.getName().trim());
    }
    if (request.getSlug() != null) entity.setSlug(resolveSlugForUpdate(request.getSlug(), companyProjectId));
    if (request.getShortDescription() != null) entity.setShortDescription(clean(request.getShortDescription()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getCityId() != null) entity.setCity(resolveCity(request.getCityId()));
    if (request.getAddressLine() != null) entity.setAddressLine(clean(request.getAddressLine()));
    if (request.getLocationLabel() != null) entity.setLocationLabel(clean(request.getLocationLabel()));
    if (request.getClientName() != null) entity.setClientName(clean(request.getClientName()));
    if (request.getProjectArea() != null) entity.setProjectArea(clean(request.getProjectArea()));
    if (request.getDetail3() != null) entity.setDetail3(clean(request.getDetail3()));
    if (request.getTags() != null) entity.setTags(joinTags(request.getTags()));
    if (request.getCoverMediaUrl() != null) entity.setCoverMediaUrl(clean(request.getCoverMediaUrl()));
    if (request.getCoverMediaType() != null) entity.setCoverMediaType(clean(request.getCoverMediaType()));
    if (request.getStats() != null) entity.setStats(request.getStats());
    if (request.getBudget() != null) entity.setBudget(request.getBudget());
    if (request.getPriceBreakdown() != null) entity.setPriceBreakdown(request.getPriceBreakdown());
    if (request.getClientRequirements() != null) entity.setClientRequirements(request.getClientRequirements());
    if (request.getDesignMaterials() != null) entity.setDesignMaterials(request.getDesignMaterials());
    if (request.getActive() != null) entity.setActive(request.getActive());
    if (request.getPublished() != null) entity.setPublished(request.getPublished());
    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    return toDetail(companyProjectRepository.save(entity));
  }

  @Override
  @Transactional
  public void softDelete(Long companyProjectId) {
    CompanyProjectEntity entity = companyProjectRepository.findByIdAndDeletedFalse(companyProjectId)
        .orElseThrow(() -> new EntityNotFoundException("Company project not found: " + companyProjectId));
    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublished(false);
    companyProjectRepository.save(entity);
  }

  private CompanyProjectDetailResponse toDetail(CompanyProjectEntity p) {
    CompanyEntity c = p.getCompany();
    return CompanyProjectDetailResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .slug(p.getSlug())
        .shortDescription(p.getShortDescription())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyType(c != null ? c.getCompanyType() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .locationLabel(p.getLocationLabel())
        .clientName(p.getClientName())
        .projectArea(p.getProjectArea())
        .detail3(p.getDetail3())
        .tags(CompanyProjectTagMapper.toTags(p.getTags()))
        .description(p.getDescription())
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .stats(p.getStats())
        .budget(p.getBudget())
        .priceBreakdown(p.getPriceBreakdown())
        .clientRequirements(p.getClientRequirements())
        .designMaterials(p.getDesignMaterials())
        .active(Boolean.TRUE.equals(p.getActive()))
        .published(Boolean.TRUE.equals(p.getPublished()))
        .priority(p.getPriority() != null ? p.getPriority() : 0)
        .deleted(Boolean.TRUE.equals(p.getDeleted()))
        .build();
  }

  private CompanyEntity resolveCompany(Long companyId) {
    return companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
  }

  private CityEntity resolveCity(Long cityId) {
    if (cityId == null) return null;
    return cityRepository.findById(cityId)
        .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));
  }

  private String clean(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String joinTags(List<String> values) {
    if (values == null) return null;
    String joined = values.stream().filter(StringUtils::hasText).map(String::trim).distinct()
        .reduce((a, b) -> a + "," + b).orElse("");
    return joined.isEmpty() ? null : joined;
  }

  private String defaultMediaType(String mediaType, String mediaUrl) {
    if (StringUtils.hasText(mediaType)) return mediaType.trim();
    return StringUtils.hasText(mediaUrl) ? "IMAGE" : null;
  }

  private String resolveSlugForCreate(String requested, String name) {
    if (StringUtils.hasText(requested)) {
      companyProjectRepository.findBySlug(requested).ifPresent(row -> {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Company project slug already exists: " + requested);
      });
      return requested;
    }
    String base = slugify(name);
    String candidate = base;
    int suffix = 2;
    while (companyProjectRepository.findBySlug(candidate).isPresent()) candidate = base + "-" + suffix++;
    return candidate;
  }

  private String resolveSlugForUpdate(String requested, Long id) {
    if (!StringUtils.hasText(requested)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug cannot be blank");
    String cleaned = requested.trim();
    companyProjectRepository.findBySlugAndIdNot(cleaned, id).ifPresent(row -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Company project slug already exists: " + cleaned);
    });
    return cleaned;
  }

  private String slugify(String input) {
    String value = input == null ? "" : input.toLowerCase().trim()
        .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    return value.isEmpty() ? "company-project" : value;
  }

  private Map<Long, Long> batchBrandsUsedCounts(List<Long> companyProjectIds) {
    Map<Long, Long> result = new HashMap<>();
    if (companyProjectIds.isEmpty()) return result;

    for (Object[] row : brandCollaborationRepository.countBrandsUsedByCompanyProjectIds(companyProjectIds)) {
      result.put((Long) row[0], (Long) row[1]);
    }
    return result;
  }

  private CompanyProjectListItemResponse toListItem(CompanyProjectEntity p, long brandsUsedCount) {
    CompanyEntity c = p.getCompany();
    return CompanyProjectListItemResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyType(c != null ? c.getCompanyType() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .coverMediaUrl(p.getCoverMediaUrl())
        .active(Boolean.TRUE.equals(p.getActive()))
        .deleted(Boolean.TRUE.equals(p.getDeleted()))
        .createdAt(p.getCreatedAt())
        .updatedAt(p.getUpdatedAt())
        .brandsUsedCount(brandsUsedCount)
        .build();
  }
}
