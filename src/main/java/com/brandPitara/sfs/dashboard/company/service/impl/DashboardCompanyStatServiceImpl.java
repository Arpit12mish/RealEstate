package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyStatEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.company.repository.CompanyStatRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyStatService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardCompanyStatServiceImpl implements DashboardCompanyStatService {

  private final CompanyRepository companyRepository;
  private final CompanyStatRepository companyStatRepository;

  @Override
  @Transactional(readOnly = true)
  public List<CompanyStatResponse> list(Long companyId) {
    assertCompanyExists(companyId);
    return fetchOrdered(companyId);
  }

  @Override
  @Transactional
  public CompanyStatResponse create(Long companyId, CompanyStatCreateRequest request) {
    CompanyEntity company = assertCompanyExists(companyId);

    CompanyStatEntity entity = CompanyStatEntity.builder()
        .company(company)
        .label(request.getLabel().trim())
        .value(request.getValue().trim())
        .iconKey(clean(request.getIconKey()))
        .displayOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .publicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : true)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    CompanyStatEntity saved = companyStatRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public CompanyStatResponse update(Long companyId, Long statId, CompanyStatUpdateRequest request) {
    CompanyStatEntity entity = findOrThrow(companyId, statId);

    if (request.getLabel() != null) {
      if (!StringUtils.hasText(request.getLabel())) {
        throw new IllegalArgumentException("label cannot be blank");
      }
      entity.setLabel(request.getLabel().trim());
    }
    if (request.getValue() != null) {
      if (!StringUtils.hasText(request.getValue())) {
        throw new IllegalArgumentException("value cannot be blank");
      }
      entity.setValue(request.getValue().trim());
    }
    if (request.getIconKey() != null) entity.setIconKey(clean(request.getIconKey()));
    if (request.getSortOrder() != null) entity.setDisplayOrder(request.getSortOrder());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getActive() != null) entity.setActive(request.getActive());

    CompanyStatEntity saved = companyStatRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long companyId, Long statId) {
    CompanyStatEntity entity = findOrThrow(companyId, statId);
    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    companyStatRepository.save(entity);
  }

  @Override
  @Transactional
  public List<CompanyStatResponse> reorder(Long companyId, CompanyStatReorderRequest request) {
    assertCompanyExists(companyId);

    Map<Long, Integer> sortOrderByStatId = request.getItems().stream()
        .collect(Collectors.toMap(
            CompanyStatReorderRequest.Item::getStatId,
            CompanyStatReorderRequest.Item::getSortOrder
        ));

    for (Map.Entry<Long, Integer> entry : sortOrderByStatId.entrySet()) {
      CompanyStatEntity entity = findOrThrow(companyId, entry.getKey());
      entity.setDisplayOrder(entry.getValue());
      companyStatRepository.save(entity);
    }

    return fetchOrdered(companyId);
  }

  // ---------- helpers ----------

  private CompanyEntity assertCompanyExists(Long companyId) {
    return companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
  }

  private CompanyStatEntity findOrThrow(Long companyId, Long statId) {
    return companyStatRepository.findByIdAndCompany_IdAndDeletedFalse(statId, companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company stat not found: " + statId));
  }

  private List<CompanyStatResponse> fetchOrdered(Long companyId) {
    return companyStatRepository.findByCompany_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private CompanyStatResponse toResponse(CompanyStatEntity e) {
    return CompanyStatResponse.builder()
        .id(e.getId())
        .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
        .label(e.getLabel())
        .value(e.getValue())
        .iconKey(e.getIconKey())
        .sortOrder(e.getDisplayOrder() != null ? e.getDisplayOrder() : 0)
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
