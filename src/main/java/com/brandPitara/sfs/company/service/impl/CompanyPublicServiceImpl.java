package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.company.dto.CompanyResponse;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.company.service.CompanyPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CompanyPublicServiceImpl implements CompanyPublicService {

  private final CompanyRepository companyRepo;

  @Override
  public CompanyResponse publicGet(Long companyId) {
    CompanyEntity c = companyRepo.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(companyId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Company not found"));

    return toResponse(c);
  }

  @Override
  public Page<CompanyResponse> publicList(String companyType, Pageable pageable) {
    Page<CompanyEntity> page = (companyType == null || companyType.isBlank())
        ? companyRepo.findByActiveTrueAndPublishedTrueAndDeletedFalse(pageable)
        : companyRepo.findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(companyType, pageable);

    return page.map(this::toResponse);
  }

  private CompanyResponse toResponse(CompanyEntity c) {
    return CompanyResponse.builder()
        .id(c.getId())
        .name(c.getName())
        .slug(c.getSlug())
        .companyType(c.getCompanyType())
        .logoUrl(c.getLogoUrl())
        .coverImageUrl(c.getCoverImageUrl())
        .description(c.getDescription())
        .phone(c.getPhone())
        .whatsapp(c.getWhatsapp())
        .email(c.getEmail())
        .build();
  }
}