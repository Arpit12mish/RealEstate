package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.distributor.dto.DistributorCardResponse;
import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import com.brandPitara.sfs.distributor.repository.DistributorRepository;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TopDistributorsSectionLoader implements HomeSectionLoader {

  private final DistributorRepository distributorRepository;
  private final BrandDistributorRepository brandDistributorRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.TOP_DISTRIBUTORS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    Long cityId = ctx.cityId();

    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 20);
    String mode = cfg.getParam1() != null ? cfg.getParam1().trim().toUpperCase() : "CITY";

    List<DistributorCardResponse> items;

    if ("BRAND".equals(mode)) {
      if (cfg.getParam2() == null) {
        items = List.of();
      } else {
        Long brandId = Long.valueOf(cfg.getParam2());
        items = brandDistributorRepository
            .findPublicDistributorCardsByBrand(brandId, cityId, PageRequest.of(0, limit))
            .getContent();
      }
    } else {
      // CITY mode: return "cards" shape too (recommended)
      if (cityId == null) {
        items = List.of();
      } else {
        // If you already have DistributorCardResponse mapper, use it.
        // For now, minimal mapping from entity -> response
        List<DistributorEntity> entities =
            distributorRepository.findByCityIdAndActiveTrueAndDeletedFalse(cityId, PageRequest.of(0, limit))
                .getContent();

        items = entities.stream()
            .map(d -> new DistributorCardResponse(
                d.getId(),
                d.getName(),
                d.getPhone(),
                d.getWhatsapp(),
                d.getCityId(),
                0,     // priority (not present in distributor table)
                null,  // offerTitle
                null,  // offerBannerUrl
                null   // validTill
            ))
            .toList();
      }
    }

    return HomeSectionDto.<DistributorCardResponse>builder()
        .type(HomeSectionType.TOP_DISTRIBUTORS)
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Distributors")
        .items(items)
        .build();
  }
}
