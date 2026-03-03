package com.brandPitara.sfs.builder.mapper;

import com.brandPitara.sfs.builder.dto.BuilderCardDto;
import com.brandPitara.sfs.builder.entity.BuilderEntity;

public class BuilderCardMapper {
  public static BuilderCardDto toCard(BuilderEntity e) {
    return BuilderCardDto.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .cityId(e.getCity() != null ? e.getCity().getId() : null)
        .cityName(e.getCity() != null ? e.getCity().getName() : null)
        .build();
  }
}
