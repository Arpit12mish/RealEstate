package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.service.PublicProjectNearbyListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NearbyListingsSectionLoader implements HomeSectionLoader {

  private static final String DEFAULT_TITLE = "Nearby Listings";
  private static final String GLOBAL_FALLBACK_TITLE = "Recommended Projects";
  private static final String GLOBAL_FALLBACK_SUBTITLE = "Popular homes and projects";
  private static final String CITY_FALLBACK_SUBTITLE = "Popular projects in your selected city";

  private final PublicProjectNearbyListingService nearbyListingService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.NEARBY_LISTINGS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.min(Math.max(cfg.getMaxItems() != null ? cfg.getMaxItems() : 5, 1), 10);

    List<ProjectNearbyListingCardDto> cards = nearbyListingService.listNearby(
        ctx != null ? ctx.latitude() : null,
        ctx != null ? ctx.longitude() : null,
        ctx != null ? ctx.cityId() : null,
        limit
    );

    return HomeSectionDto.<ProjectNearbyListingCardDto>builder()
        .type(HomeSectionType.NEARBY_LISTINGS)
        .key("NEARBY_LISTINGS")
        .title(resolveTitle(ctx))
        .subtitle(resolveSubtitle(cfg, ctx))
        .items(cards == null ? List.of() : cards)
        .build();
  }

  private String resolveTitle(SectionContext ctx) {
    if (ctx != null && ctx.hasUserCoordinates()) {
      return DEFAULT_TITLE;
    }

    if (hasSelectedCityContext(ctx)) {
      String cityName = firstNonBlank(ctx.resolvedCityName(), ctx.deviceCity());
      return hasText(cityName) ? "Projects in " + cityName.trim() : "Projects in your city";
    }

    return GLOBAL_FALLBACK_TITLE;
  }

  private String resolveSubtitle(HomeSectionConfigEntity cfg, SectionContext ctx) {
    if (ctx != null && ctx.hasUserCoordinates()) {
      if (hasText(ctx.resolvedCityName())) {
        return "Around " + ctx.resolvedCityName().trim();
      }
      return "Near you";
    }

    if (hasSelectedCityContext(ctx)) {
      return CITY_FALLBACK_SUBTITLE;
    }

    return GLOBAL_FALLBACK_SUBTITLE;
  }

  private boolean hasSelectedCityContext(SectionContext ctx) {
    return ctx != null
        && !ctx.hasUserCoordinates()
        && ctx.cityId() != null;
  }

  private String firstNonBlank(String first, String second) {
    return hasText(first) ? first : second;
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
