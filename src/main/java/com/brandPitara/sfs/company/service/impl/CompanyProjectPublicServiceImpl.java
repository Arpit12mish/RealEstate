package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.service.CompanyProjectPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CompanyProjectPublicServiceImpl implements CompanyProjectPublicService {

  private final CompanyProjectRepository repo;

  @Override
  public Page<CompanyProjectCardDto> publicListByCompany(Long companyId, Pageable pageable) {
    return repo.findByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(companyId, pageable)
        .map(this::toCard);
  }

  @Override
  public CompanyProjectResponse publicGet(Long companyProjectId) {
    CompanyProjectEntity p = repo.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(companyProjectId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Company project not found"));

    return toResponse(p);
  }

  private CompanyProjectCardDto toCard(CompanyProjectEntity p) {
    CompanyEntity c = p.getCompany();
    return CompanyProjectCardDto.builder()
        .id(p.getId())
        .name(p.getName())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyLogoUrl(c != null ? c.getLogoUrl() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .build();
  }

  private CompanyProjectResponse toResponse(CompanyProjectEntity p) {
    CompanyEntity c = p.getCompany();
    return CompanyProjectResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .slug(p.getSlug())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyLogoUrl(c != null ? c.getLogoUrl() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .description(p.getDescription())
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .build();
  }
}