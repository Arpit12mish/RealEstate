package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyMediaEntity;
import com.brandPitara.sfs.company.repository.CompanyMediaRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyMediaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardCompanyMediaServiceImpl implements DashboardCompanyMediaService {

  private static final Set<String> ALLOWED_USAGE_TYPES = Set.of("HERO", "GALLERY", "CARD");

  private final CompanyRepository companyRepository;
  private final CompanyMediaRepository companyMediaRepository;

  @Override
  @Transactional(readOnly = true)
  public List<CompanyMediaResponse> list(Long companyId) {
    assertCompanyExists(companyId);
    return fetchOrdered(companyId);
  }

  @Override
  @Transactional
  public CompanyMediaResponse create(Long companyId, CompanyMediaCreateRequest request) {
    CompanyEntity company = assertCompanyExists(companyId);
    String usageType = assertValidUsageType(request.getUsageType());

    CompanyMediaEntity entity = CompanyMediaEntity.builder()
        .company(company)
        .mediaUrl(request.getMediaUrl().trim())
        .mediaType(StringUtils.hasText(request.getMediaType()) ? request.getMediaType().trim() : "IMAGE")
        .usageType(usageType)
        .title(clean(request.getTitle()))
        .altText(clean(request.getAltText()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .publicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : true)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    CompanyMediaEntity saved = companyMediaRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public CompanyMediaResponse update(Long companyId, Long mediaId, CompanyMediaUpdateRequest request) {
    CompanyMediaEntity entity = findOrThrow(companyId, mediaId);

    if (request.getMediaUrl() != null) {
      if (!StringUtils.hasText(request.getMediaUrl())) {
        throw new IllegalArgumentException("mediaUrl cannot be blank");
      }
      entity.setMediaUrl(request.getMediaUrl().trim());
    }
    if (request.getMediaType() != null) entity.setMediaType(request.getMediaType().trim());
    if (request.getUsageType() != null) entity.setUsageType(assertValidUsageType(request.getUsageType()));
    if (request.getTitle() != null) entity.setTitle(clean(request.getTitle()));
    if (request.getAltText() != null) entity.setAltText(clean(request.getAltText()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getActive() != null) entity.setActive(request.getActive());

    CompanyMediaEntity saved = companyMediaRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long companyId, Long mediaId) {
    CompanyMediaEntity entity = findOrThrow(companyId, mediaId);
    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    companyMediaRepository.save(entity);
  }

  @Override
  @Transactional
  public List<CompanyMediaResponse> reorder(Long companyId, CompanyMediaReorderRequest request) {
    assertCompanyExists(companyId);

    Map<Long, Integer> sortOrderByMediaId = request.getItems().stream()
        .collect(Collectors.toMap(
            CompanyMediaReorderRequest.Item::getMediaId,
            CompanyMediaReorderRequest.Item::getSortOrder
        ));

    for (Map.Entry<Long, Integer> entry : sortOrderByMediaId.entrySet()) {
      CompanyMediaEntity entity = findOrThrow(companyId, entry.getKey());
      entity.setSortOrder(entry.getValue());
      companyMediaRepository.save(entity);
    }

    return fetchOrdered(companyId);
  }

  // ---------- helpers ----------

  private CompanyEntity assertCompanyExists(Long companyId) {
    return companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
  }

  private String assertValidUsageType(String usageType) {
    String normalized = usageType == null ? "" : usageType.trim().toUpperCase();
    if (!ALLOWED_USAGE_TYPES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "usageType must be one of " + ALLOWED_USAGE_TYPES
      );
    }
    return normalized;
  }

  private CompanyMediaEntity findOrThrow(Long companyId, Long mediaId) {
    return companyMediaRepository.findByIdAndCompany_IdAndDeletedFalse(mediaId, companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company media not found: " + mediaId));
  }

  private List<CompanyMediaResponse> fetchOrdered(Long companyId) {
    return companyMediaRepository.findByCompany_IdAndDeletedFalseOrderBySortOrderAscIdAsc(companyId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private CompanyMediaResponse toResponse(CompanyMediaEntity e) {
    return CompanyMediaResponse.builder()
        .id(e.getId())
        .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
        .mediaUrl(e.getMediaUrl())
        .mediaType(e.getMediaType())
        .usageType(e.getUsageType())
        .title(e.getTitle())
        .altText(e.getAltText())
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
