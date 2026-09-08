package com.brandPitara.sfs.dashboard.companyproject.service.impl;

import com.brandPitara.sfs.company.dto.CompanyProjectMediaResponse;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectMediaEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectMediaRepository;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.dashboard.companyproject.dto.*;
import com.brandPitara.sfs.dashboard.companyproject.service.DashboardCompanyProjectMediaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCompanyProjectMediaServiceImpl implements DashboardCompanyProjectMediaService {
  private final CompanyProjectRepository companyProjectRepository;
  private final CompanyProjectMediaRepository companyProjectMediaRepository;

  @Override @Transactional(readOnly = true)
  public List<CompanyProjectMediaResponse> list(Long companyProjectId) {
    assertProjectExists(companyProjectId);
    return fetchOrdered(companyProjectId);
  }

  @Override @Transactional
  public CompanyProjectMediaResponse create(Long companyProjectId, CompanyProjectMediaCreateRequest request) {
    CompanyProjectEntity project = assertProjectExists(companyProjectId);
    CompanyProjectMediaEntity entity = CompanyProjectMediaEntity.builder()
        .companyProject(project)
        .mediaUrl(request.getMediaUrl().trim())
        .mediaType(StringUtils.hasText(request.getMediaType()) ? request.getMediaType().trim().toUpperCase() : "IMAGE")
        .categoryKey(normalizeRequired(request.getCategoryKey(), "categoryKey"))
        .categoryLabel(clean(request.getCategoryLabel()))
        .title(clean(request.getTitle()))
        .description(clean(request.getDescription()))
        .metricLabel(clean(request.getMetricLabel()))
        .metricValue(clean(request.getMetricValue()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .publicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : true)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();
    return toResponse(companyProjectMediaRepository.save(entity));
  }

  @Override @Transactional
  public CompanyProjectMediaResponse update(Long companyProjectId, Long mediaId, CompanyProjectMediaUpdateRequest request) {
    CompanyProjectMediaEntity entity = findOrThrow(companyProjectId, mediaId);
    if (request.getMediaUrl() != null) entity.setMediaUrl(normalizeRequired(request.getMediaUrl(), "mediaUrl"));
    if (request.getMediaType() != null) entity.setMediaType(normalizeRequired(request.getMediaType(), "mediaType").toUpperCase());
    if (request.getCategoryKey() != null) entity.setCategoryKey(normalizeRequired(request.getCategoryKey(), "categoryKey"));
    if (request.getCategoryLabel() != null) entity.setCategoryLabel(clean(request.getCategoryLabel()));
    if (request.getTitle() != null) entity.setTitle(clean(request.getTitle()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getMetricLabel() != null) entity.setMetricLabel(clean(request.getMetricLabel()));
    if (request.getMetricValue() != null) entity.setMetricValue(clean(request.getMetricValue()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getActive() != null) entity.setActive(request.getActive());
    return toResponse(companyProjectMediaRepository.save(entity));
  }

  @Override @Transactional
  public void delete(Long companyProjectId, Long mediaId) {
    CompanyProjectMediaEntity entity = findOrThrow(companyProjectId, mediaId);
    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    companyProjectMediaRepository.save(entity);
  }

  @Override @Transactional
  public List<CompanyProjectMediaResponse> reorder(Long companyProjectId, CompanyProjectMediaReorderRequest request) {
    assertProjectExists(companyProjectId);
    for (CompanyProjectMediaReorderRequest.Item item : request.getItems()) {
      CompanyProjectMediaEntity entity = findOrThrow(companyProjectId, item.getMediaId());
      entity.setSortOrder(item.getSortOrder());
      companyProjectMediaRepository.save(entity);
    }
    return fetchOrdered(companyProjectId);
  }

  private CompanyProjectEntity assertProjectExists(Long id) {
    return companyProjectRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Company project not found: " + id));
  }

  private CompanyProjectMediaEntity findOrThrow(Long projectId, Long mediaId) {
    return companyProjectMediaRepository.findByIdAndCompanyProject_IdAndDeletedFalse(mediaId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Company project media not found: " + mediaId));
  }

  private List<CompanyProjectMediaResponse> fetchOrdered(Long id) {
    return companyProjectMediaRepository.findByCompanyProject_IdAndDeletedFalseOrderBySortOrderAscIdAsc(id)
        .stream().map(this::toResponse).toList();
  }

  private CompanyProjectMediaResponse toResponse(CompanyProjectMediaEntity e) {
    return CompanyProjectMediaResponse.builder()
        .id(e.getId()).companyProjectId(e.getCompanyProject().getId())
        .mediaUrl(e.getMediaUrl()).mediaType(e.getMediaType())
        .categoryKey(e.getCategoryKey()).categoryLabel(e.getCategoryLabel())
        .title(e.getTitle()).description(e.getDescription())
        .metricLabel(e.getMetricLabel()).metricValue(e.getMetricValue())
        .sortOrder(e.getSortOrder()).publicVisible(e.getPublicVisible()).active(e.getActive())
        .build();
  }

  private String clean(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
  private String normalizeRequired(String value, String field) {
    if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " cannot be blank");
    return value.trim();
  }
}
