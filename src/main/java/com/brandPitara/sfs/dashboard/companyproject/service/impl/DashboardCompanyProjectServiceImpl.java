package com.brandPitara.sfs.dashboard.companyproject.service.impl;

import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectDetailResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectListItemResponse;
import com.brandPitara.sfs.dashboard.companyproject.service.DashboardCompanyProjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardCompanyProjectServiceImpl implements DashboardCompanyProjectService {

  private final CompanyProjectRepository companyProjectRepository;
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

    CompanyEntity c = p.getCompany();
    return CompanyProjectDetailResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyType(c != null ? c.getCompanyType() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .clientName(p.getClientName())
        .projectArea(p.getProjectArea())
        .detail3(p.getDetail3())
        .tags(CompanyProjectTagMapper.toTags(p.getTags()))
        .description(p.getDescription())
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .active(Boolean.TRUE.equals(p.getActive()))
        .deleted(Boolean.TRUE.equals(p.getDeleted()))
        .build();
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
