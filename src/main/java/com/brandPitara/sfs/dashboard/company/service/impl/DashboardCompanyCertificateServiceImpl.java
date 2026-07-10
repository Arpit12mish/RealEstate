package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyCertificateEntity;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyCertificateRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyCertificateService;
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
public class DashboardCompanyCertificateServiceImpl implements DashboardCompanyCertificateService {

  private final CompanyRepository companyRepository;
  private final CompanyCertificateRepository companyCertificateRepository;

  @Override
  @Transactional(readOnly = true)
  public List<CompanyCertificateResponse> list(Long companyId) {
    assertCompanyExists(companyId);
    return fetchOrdered(companyId);
  }

  @Override
  @Transactional
  public CompanyCertificateResponse create(Long companyId, CompanyCertificateCreateRequest request) {
    CompanyEntity company = assertCompanyExists(companyId);

    CompanyCertificateEntity entity = CompanyCertificateEntity.builder()
        .company(company)
        .title(request.getTitle().trim())
        .issuer(clean(request.getIssuer()))
        .description(clean(request.getDescription()))
        .certificateUrl(clean(request.getCertificateUrl()))
        .certificateFileUrl(clean(request.getCertificateFileUrl()))
        .year(request.getYear())
        .verified(request.getVerified() != null ? request.getVerified() : false)
        .publicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : true)
        .displayOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    CompanyCertificateEntity saved = companyCertificateRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public CompanyCertificateResponse update(Long companyId, Long certificateId, CompanyCertificateUpdateRequest request) {
    CompanyCertificateEntity entity = findOrThrow(companyId, certificateId);

    if (request.getTitle() != null) {
      if (!StringUtils.hasText(request.getTitle())) {
        throw new IllegalArgumentException("title cannot be blank");
      }
      entity.setTitle(request.getTitle().trim());
    }
    if (request.getIssuer() != null) entity.setIssuer(clean(request.getIssuer()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getCertificateUrl() != null) entity.setCertificateUrl(clean(request.getCertificateUrl()));
    if (request.getCertificateFileUrl() != null) entity.setCertificateFileUrl(clean(request.getCertificateFileUrl()));
    if (request.getYear() != null) entity.setYear(request.getYear());
    if (request.getVerified() != null) entity.setVerified(request.getVerified());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getSortOrder() != null) entity.setDisplayOrder(request.getSortOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    CompanyCertificateEntity saved = companyCertificateRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long companyId, Long certificateId) {
    CompanyCertificateEntity entity = findOrThrow(companyId, certificateId);
    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    companyCertificateRepository.save(entity);
  }

  @Override
  @Transactional
  public List<CompanyCertificateResponse> reorder(Long companyId, CompanyCertificateReorderRequest request) {
    assertCompanyExists(companyId);

    Map<Long, Integer> sortOrderByCertificateId = request.getItems().stream()
        .collect(Collectors.toMap(
            CompanyCertificateReorderRequest.Item::getCertificateId,
            CompanyCertificateReorderRequest.Item::getSortOrder
        ));

    for (Map.Entry<Long, Integer> entry : sortOrderByCertificateId.entrySet()) {
      CompanyCertificateEntity entity = findOrThrow(companyId, entry.getKey());
      entity.setDisplayOrder(entry.getValue());
      companyCertificateRepository.save(entity);
    }

    return fetchOrdered(companyId);
  }

  // ---------- helpers ----------

  private CompanyEntity assertCompanyExists(Long companyId) {
    return companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
  }

  private CompanyCertificateEntity findOrThrow(Long companyId, Long certificateId) {
    return companyCertificateRepository.findByIdAndCompany_IdAndDeletedFalse(certificateId, companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company certificate not found: " + certificateId));
  }

  private List<CompanyCertificateResponse> fetchOrdered(Long companyId) {
    return companyCertificateRepository.findByCompany_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private CompanyCertificateResponse toResponse(CompanyCertificateEntity e) {
    return CompanyCertificateResponse.builder()
        .id(e.getId())
        .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
        .title(e.getTitle())
        .issuer(e.getIssuer())
        .description(e.getDescription())
        .certificateUrl(e.getCertificateUrl())
        .certificateFileUrl(e.getCertificateFileUrl())
        .year(e.getYear())
        .verified(Boolean.TRUE.equals(e.getVerified()))
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .sortOrder(e.getDisplayOrder() != null ? e.getDisplayOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
