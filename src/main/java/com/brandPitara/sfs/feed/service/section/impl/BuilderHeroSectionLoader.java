package com.brandPitara.sfs.feed.service.section.impl;

import com.brandPitara.sfs.builder.dto.BuilderAboutDto;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import com.brandPitara.sfs.feed.service.section.FeedContext;
import com.brandPitara.sfs.feed.service.section.FeedSectionLoader;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BuilderHeroSectionLoader implements FeedSectionLoader {

  private final BuilderRepository builderRepository;

  @Override
  public String supports() {
    return "BUILDER_HERO";
  }

  @Override
  public HomeSectionDto<?> load(FeedSectionConfigEntity cfg, FeedContext ctx) {
    if (ctx == null || ctx.entityId() == null) {
      System.out.println("❌ BUILDER_HERO: ctx/entityId missing");
      return null;
    }

    var builder = builderRepository.findByIdAndDeletedFalse(ctx.entityId()).orElse(null);
    if (builder == null) {
      System.out.println("❌ BUILDER_HERO: builder not found id=" + ctx.entityId());
      return null;
    }

    var city = builder.getCity();

    BuilderAboutDto about = BuilderAboutDto.builder()
        .id(builder.getId())
        .name(builder.getName())
        .logoUrl(builder.getLogoUrl())
        .description(builder.getDescription())
        .phone(builder.getPhone())
        .whatsapp(builder.getWhatsapp())
        .email(builder.getEmail())
        .addressLine(builder.getAddressLine())
        .cityId(city == null ? null : city.getId())
        .cityName(city == null ? null : city.getName())
        .latitude(builder.getLatitude())
        .longitude(builder.getLongitude())
        .active(builder.getActive())
        .published(builder.getPublished())
        .priority(builder.getPriority())
        .build();

    var section = HomeSectionDto.<BuilderAboutDto>builder()
        .key("ABOUT") // anchor for promo insertion if you want
        .type(HomeSectionType.BUILDER_HERO)
        .title(cfg == null ? null : cfg.getTitle())
        .items(List.of(about))
        .build();

    System.out.println("✅ BUILDER_HERO: built items=1 key=" + section.getKey() + " type=" + section.getType());
    return section;
  }
}