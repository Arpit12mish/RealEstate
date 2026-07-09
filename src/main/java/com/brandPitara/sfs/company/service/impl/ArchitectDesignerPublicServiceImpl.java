package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.entity.*;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.repository.*;
import com.brandPitara.sfs.company.service.ArchitectDesignerPublicService;
import com.brandPitara.sfs.company.service.CompanyConnectedBrandPublicService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ArchitectDesignerPublicServiceImpl implements ArchitectDesignerPublicService {

  private final CompanyRepository companyRepository;
  private final CompanyProjectRepository companyProjectRepository;
  private final CompanyStatRepository companyStatRepository;
  private final CompanyAwardRepository companyAwardRepository;
  private final CompanyCertificateRepository companyCertificateRepository;
  private final CompanyConnectedBrandPublicService companyConnectedBrandPublicService;

  @Override
  public ArchitectDesignerDetailResponse getDetail(Long companyId) {
    CompanyEntity company = companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(companyId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Architect/Designer not found"));

    List<CompanyProjectCardDto> topProjects = companyProjectRepository
        .findTop10ByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(companyId)
        .stream()
        .map(this::toProjectCard)
        .toList();

    List<CompanyStatDto> stats = companyStatRepository
        .findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(s -> CompanyStatDto.builder()
            .label(s.getLabel())
            .value(s.getValue())
            .build())
        .toList();

    List<CompanyAwardDto> awards = companyAwardRepository
        .findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(a -> CompanyAwardDto.builder()
            .id(a.getId())
            .title(a.getTitle())
            .subtitle(a.getSubtitle())
            .description(a.getDescription())
            .displayOrder(a.getDisplayOrder())
            .build())
        .toList();

    List<CompanyCertificateDto> certificates = companyCertificateRepository
        .findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(c -> CompanyCertificateDto.builder()
            .id(c.getId())
            .title(c.getTitle())
            .issuer(c.getIssuer())
            .certificateUrl(c.getCertificateUrl())
            .displayOrder(c.getDisplayOrder())
            .build())
        .toList();

    List<ConnectedBrandDto> connectedBrands = companyConnectedBrandPublicService.getConnectedBrands(companyId);

    return ArchitectDesignerDetailResponse.builder()
        .companyId(company.getId())
        .name(company.getName())
        .slug(company.getSlug())
        .companyType(company.getCompanyType())
        .logoUrl(company.getLogoUrl())
        .thumbnailImageUrl(company.getCoverImageUrl())
        .description(company.getDescription())
        .topProjects(topProjects)
        .stats(stats)
        .awardsAndPublications(awards)
        .certificates(certificates)
        .connectedBrands(connectedBrands)
        .build();
  }

  private CompanyProjectCardDto toProjectCard(CompanyProjectEntity p) {
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
        .projectCityLatitude(p.getCity() != null ? p.getCity().getLatitude() : null)
        .projectCityLongitude(p.getCity() != null ? p.getCity().getLongitude() : null)
        .clientName(p.getClientName())
        .projectArea(p.getProjectArea())
        .detail3(p.getDetail3())
        .tags(CompanyProjectTagMapper.toTags(p.getTags()))
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .build();
  }
}
