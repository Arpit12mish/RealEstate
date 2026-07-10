package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyDetailResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyListItemResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyService;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardCompanyServiceImpl implements DashboardCompanyService {

  private final CompanyRepository companyRepository;
  private final CityRepository cityRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<CompanyListItemResponse> list(
      String q,
      String companyType,
      Long cityId,
      Boolean active,
      Boolean published,
      Pageable pageable
  ) {
    Page<CompanyEntity> page = StringUtils.hasText(q)
        ? companyRepository.searchForDashboardByName(q.trim(), companyType, cityId, active, published, pageable)
        : companyRepository.searchForDashboard(companyType, cityId, active, published, pageable);

    List<Long> ids = page.getContent().stream().map(CompanyEntity::getId).toList();
    Map<Long, Long> connectedBrandsCountById = batchConnectedBrandsCounts(ids);

    return page.map(c -> toListItem(c, connectedBrandsCountById.getOrDefault(c.getId(), 0L)));
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyDetailResponse getDetail(Long companyId) {
    CompanyEntity c = companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
    return toDetail(c);
  }

  @Override
  @Transactional
  public CompanyDetailResponse create(CompanyCreateRequest request) {
    CityEntity city = resolveCity(request.getCityId());

    CompanyEntity entity = CompanyEntity.builder()
        .name(request.getName().trim())
        .slug(resolveSlugForCreate(clean(request.getSlug()), request.getName()))
        .companyType(request.getCompanyType().trim())
        .logoUrl(clean(request.getLogoUrl()))
        .coverImageUrl(clean(request.getCoverImageUrl()))
        .description(clean(request.getDescription()))
        .specializationText(clean(request.getSpecializationText()))
        .servicesOffered(joinServices(request.getServicesOffered()))
        .city(city)
        .addressLine(clean(request.getAddressLine()))
        .phone(clean(request.getPhone()))
        .whatsapp(clean(request.getWhatsapp()))
        .email(clean(request.getEmail()))
        .priority(request.getPriority() != null ? request.getPriority() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .published(request.getPublished() != null ? request.getPublished() : false)
        .deleted(false)
        .build();

    CompanyEntity saved = companyRepository.save(entity);
    return toDetail(saved);
  }

  @Override
  @Transactional
  public CompanyDetailResponse update(Long companyId, CompanyUpdateRequest request) {
    CompanyEntity entity = companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));

    if (request.getName() != null) {
      if (!StringUtils.hasText(request.getName())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
      }
      entity.setName(request.getName().trim());
    }
    if (request.getSlug() != null) {
      entity.setSlug(resolveSlugForUpdate(request.getSlug(), companyId));
    }
    if (request.getCompanyType() != null) {
      if (!StringUtils.hasText(request.getCompanyType())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyType cannot be blank");
      }
      entity.setCompanyType(request.getCompanyType().trim());
    }
    if (request.getLogoUrl() != null) entity.setLogoUrl(clean(request.getLogoUrl()));
    if (request.getCoverImageUrl() != null) entity.setCoverImageUrl(clean(request.getCoverImageUrl()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getSpecializationText() != null) entity.setSpecializationText(clean(request.getSpecializationText()));
    if (request.getServicesOffered() != null) entity.setServicesOffered(joinServices(request.getServicesOffered()));
    if (request.getCityId() != null) entity.setCity(resolveCity(request.getCityId()));
    if (request.getAddressLine() != null) entity.setAddressLine(clean(request.getAddressLine()));
    if (request.getPhone() != null) entity.setPhone(clean(request.getPhone()));
    if (request.getWhatsapp() != null) entity.setWhatsapp(clean(request.getWhatsapp()));
    if (request.getEmail() != null) entity.setEmail(clean(request.getEmail()));
    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getActive() != null) entity.setActive(request.getActive());
    if (request.getPublished() != null) entity.setPublished(request.getPublished());

    CompanyEntity saved = companyRepository.save(entity);
    return toDetail(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long companyId) {
    CompanyEntity entity = companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));

    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublished(false);
    companyRepository.save(entity);
  }

  // ---------- helpers ----------

  private CityEntity resolveCity(Long cityId) {
    if (cityId == null) return null;
    return cityRepository.findById(cityId)
        .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));
  }

  private Map<Long, Long> batchConnectedBrandsCounts(List<Long> companyIds) {
    Map<Long, Long> result = new HashMap<>();
    if (companyIds.isEmpty()) return result;

    for (Object[] row : brandCollaborationRepository.countConnectedBrandsByCompanyIds(companyIds)) {
      result.put((Long) row[0], (Long) row[1]);
    }
    return result;
  }

  private CompanyListItemResponse toListItem(CompanyEntity c, long connectedBrandsCount) {
    return CompanyListItemResponse.builder()
        .id(c.getId())
        .name(c.getName())
        .slug(c.getSlug())
        .companyType(c.getCompanyType())
        .logoUrl(c.getLogoUrl())
        .cityId(c.getCity() != null ? c.getCity().getId() : null)
        .cityName(c.getCity() != null ? c.getCity().getName() : null)
        .active(Boolean.TRUE.equals(c.getActive()))
        .published(Boolean.TRUE.equals(c.getPublished()))
        .deleted(Boolean.TRUE.equals(c.getDeleted()))
        .createdAt(c.getCreatedAt())
        .updatedAt(c.getUpdatedAt())
        .connectedBrandsCount(connectedBrandsCount)
        .build();
  }

  private CompanyDetailResponse toDetail(CompanyEntity c) {
    return CompanyDetailResponse.builder()
        .id(c.getId())
        .name(c.getName())
        .slug(c.getSlug())
        .companyType(c.getCompanyType())
        .logoUrl(c.getLogoUrl())
        .coverImageUrl(c.getCoverImageUrl())
        .description(c.getDescription())
        .specializationText(c.getSpecializationText())
        .servicesOffered(CompanyProjectTagMapper.toTags(c.getServicesOffered()))
        .cityId(c.getCity() != null ? c.getCity().getId() : null)
        .cityName(c.getCity() != null ? c.getCity().getName() : null)
        .addressLine(c.getAddressLine())
        .phone(c.getPhone())
        .whatsapp(c.getWhatsapp())
        .email(c.getEmail())
        .priority(c.getPriority() != null ? c.getPriority() : 0)
        .active(Boolean.TRUE.equals(c.getActive()))
        .published(Boolean.TRUE.equals(c.getPublished()))
        .deleted(Boolean.TRUE.equals(c.getDeleted()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

  private String joinServices(List<String> services) {
    if (services == null) return null;
    String joined = services.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .reduce((a, b) -> a + "," + b)
        .orElse("");
    return joined.isEmpty() ? null : joined;
  }

  // ---------- slug helpers (mirrors BrandServiceImpl's convention) ----------

  private String resolveSlugForCreate(String requestedSlug, String name) {
    if (StringUtils.hasText(requestedSlug)) {
      companyRepository.findBySlug(requestedSlug).ifPresent(existing -> {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Company slug already exists: " + requestedSlug);
      });
      return requestedSlug;
    }
    return generateUniqueSlug(name);
  }

  private String resolveSlugForUpdate(String requestedSlug, Long companyId) {
    if (!StringUtils.hasText(requestedSlug)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug cannot be blank");
    }
    companyRepository.findBySlugAndIdNot(requestedSlug, companyId).ifPresent(existing -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Company slug already exists: " + requestedSlug);
    });
    return requestedSlug;
  }

  private String generateUniqueSlug(String name) {
    String base = slugify(name);
    String candidate = base;
    int suffix = 2;
    while (companyRepository.findBySlug(candidate).isPresent()) {
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String base = input == null ? "" : input.toLowerCase().trim()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-+|-+$)", "");
    return base.isEmpty() ? "company" : base;
  }
}
