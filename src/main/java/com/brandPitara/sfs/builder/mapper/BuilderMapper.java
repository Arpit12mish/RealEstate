package com.brandPitara.sfs.builder.mapper;

import com.brandPitara.sfs.builder.dto.BuilderCardResponse;
import com.brandPitara.sfs.builder.dto.BuilderPublicResponse;
import com.brandPitara.sfs.builder.dto.BuilderResponse;
import com.brandPitara.sfs.builder.entity.BuilderEntity;

public class BuilderMapper {

  private BuilderMapper() {}

  public static BuilderResponse toResponse(BuilderEntity e) {
    Long cityId = (e.getCity() != null) ? e.getCity().getId() : null;
    String cityName = (e.getCity() != null) ? e.getCity().getName() : null;

    return BuilderResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .description(e.getDescription())
        .phone(e.getPhone())
        .whatsapp(e.getWhatsapp())
        .email(e.getEmail())
        .addressLine(e.getAddressLine())
        .cityId(cityId)
        .cityName(cityName)
        .latitude(e.getLatitude())
        .longitude(e.getLongitude())
        .active(Boolean.TRUE.equals(e.getActive()))
        .published(Boolean.TRUE.equals(e.getPublished()))
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }

  public static BuilderPublicResponse toPublicResponse(BuilderEntity e) {
    Long cityId = (e.getCity() != null) ? e.getCity().getId() : null;
    String cityName = (e.getCity() != null) ? e.getCity().getName() : null;

    return BuilderPublicResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .description(e.getDescription())
        .addressLine(e.getAddressLine())
        .cityId(cityId)
        .cityName(cityName)
        .latitude(e.getLatitude())
        .longitude(e.getLongitude())
        .build();
  }

  public static BuilderCardResponse toCard(BuilderEntity e) {

    return BuilderCardResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .build();
  }
}
