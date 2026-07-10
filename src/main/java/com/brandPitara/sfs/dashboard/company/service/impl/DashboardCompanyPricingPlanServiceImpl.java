package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyPricingPlanEntity;
import com.brandPitara.sfs.company.repository.CompanyPricingPlanRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyPricingPlanService;
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
public class DashboardCompanyPricingPlanServiceImpl implements DashboardCompanyPricingPlanService {

  private static final Set<String> ALLOWED_PRICING_TYPES = Set.of("SUBSCRIPTION", "PROJECT_BASED");

  private final CompanyRepository companyRepository;
  private final CompanyPricingPlanRepository companyPricingPlanRepository;

  @Override
  @Transactional(readOnly = true)
  public List<CompanyPricingPlanResponse> list(Long companyId) {
    assertCompanyExists(companyId);
    return fetchOrdered(companyId);
  }

  @Override
  @Transactional
  public CompanyPricingPlanResponse create(Long companyId, CompanyPricingPlanCreateRequest request) {
    CompanyEntity company = assertCompanyExists(companyId);
    String pricingType = assertValidPricingType(request.getPricingType());

    CompanyPricingPlanEntity entity = CompanyPricingPlanEntity.builder()
        .company(company)
        .pricingType(pricingType)
        .planName(request.getPlanName().trim())
        .priceAmount(request.getPriceAmount())
        .currency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim() : "INR")
        .billingUnit(clean(request.getBillingUnit()))
        .description(clean(request.getDescription()))
        .features(sanitizeFeatures(request.getFeatures()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .publicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : true)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    CompanyPricingPlanEntity saved = companyPricingPlanRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public CompanyPricingPlanResponse update(Long companyId, Long planId, CompanyPricingPlanUpdateRequest request) {
    CompanyPricingPlanEntity entity = findOrThrow(companyId, planId);

    if (request.getPricingType() != null) entity.setPricingType(assertValidPricingType(request.getPricingType()));
    if (request.getPlanName() != null) {
      if (!StringUtils.hasText(request.getPlanName())) {
        throw new IllegalArgumentException("planName cannot be blank");
      }
      entity.setPlanName(request.getPlanName().trim());
    }
    if (request.getPriceAmount() != null) entity.setPriceAmount(request.getPriceAmount());
    if (request.getCurrency() != null) entity.setCurrency(request.getCurrency().trim());
    if (request.getBillingUnit() != null) entity.setBillingUnit(clean(request.getBillingUnit()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getFeatures() != null) entity.setFeatures(sanitizeFeatures(request.getFeatures()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getActive() != null) entity.setActive(request.getActive());

    CompanyPricingPlanEntity saved = companyPricingPlanRepository.save(entity);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long companyId, Long planId) {
    CompanyPricingPlanEntity entity = findOrThrow(companyId, planId);
    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    companyPricingPlanRepository.save(entity);
  }

  @Override
  @Transactional
  public List<CompanyPricingPlanResponse> reorder(Long companyId, CompanyPricingPlanReorderRequest request) {
    assertCompanyExists(companyId);

    Map<Long, Integer> sortOrderByPlanId = request.getItems().stream()
        .collect(Collectors.toMap(
            CompanyPricingPlanReorderRequest.Item::getPlanId,
            CompanyPricingPlanReorderRequest.Item::getSortOrder
        ));

    for (Map.Entry<Long, Integer> entry : sortOrderByPlanId.entrySet()) {
      CompanyPricingPlanEntity entity = findOrThrow(companyId, entry.getKey());
      entity.setSortOrder(entry.getValue());
      companyPricingPlanRepository.save(entity);
    }

    return fetchOrdered(companyId);
  }

  // ---------- helpers ----------

  private CompanyEntity assertCompanyExists(Long companyId) {
    return companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
  }

  private String assertValidPricingType(String pricingType) {
    String normalized = pricingType == null ? "" : pricingType.trim().toUpperCase();
    if (!ALLOWED_PRICING_TYPES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "pricingType must be one of " + ALLOWED_PRICING_TYPES
      );
    }
    return normalized;
  }

  private CompanyPricingPlanEntity findOrThrow(Long companyId, Long planId) {
    return companyPricingPlanRepository.findByIdAndCompany_IdAndDeletedFalse(planId, companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company pricing plan not found: " + planId));
  }

  private List<CompanyPricingPlanResponse> fetchOrdered(Long companyId) {
    return companyPricingPlanRepository.findByCompany_IdAndDeletedFalseOrderBySortOrderAscIdAsc(companyId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private CompanyPricingPlanResponse toResponse(CompanyPricingPlanEntity e) {
    return CompanyPricingPlanResponse.builder()
        .id(e.getId())
        .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
        .pricingType(e.getPricingType())
        .planName(e.getPlanName())
        .priceAmount(e.getPriceAmount())
        .currency(e.getCurrency())
        .billingUnit(e.getBillingUnit())
        .description(e.getDescription())
        .features(e.getFeatures() != null ? e.getFeatures() : List.of())
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  // Hibernate maps this field straight to/from the jsonb column (see
  // CompanyPricingPlanEntity.features), so no manual JSON (de)serialization is
  // needed here - only trimming/blank-filtering of the incoming list.
  private List<String> sanitizeFeatures(List<String> features) {
    if (features == null) return List.of();
    return features.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .toList();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
