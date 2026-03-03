package com.brandPitara.sfs.feed.service.section.impl;

import com.brandPitara.sfs.builder.dto.BuilderStatTileDto;
import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import com.brandPitara.sfs.feed.service.section.FeedContext;
import com.brandPitara.sfs.feed.service.section.FeedSectionLoader;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BuilderStatsSectionLoader implements FeedSectionLoader {

  private final ObjectMapper objectMapper;

  @Override
  public String supports() {
    return "BUILDER_STATS";
  }

  @Override
  public HomeSectionDto<?> load(FeedSectionConfigEntity cfg, FeedContext ctx) {
    if (cfg == null) {
      System.out.println("❌ BUILDER_STATS: cfg is null");
      return null;
    }

    Object raw = cfg.getParam1(); // IMPORTANT: if your entity still has String, change to Object.
    if (raw == null) {
      System.out.println("❌ BUILDER_STATS: param1 is null");
      return null;
    }

    String json;
    try {
      if (raw instanceof String s) {
        json = s;
      } else {
        // jsonb might come as Map/List etc
        json = objectMapper.writeValueAsString(raw);
      }
    } catch (Exception e) {
      System.out.println("❌ BUILDER_STATS: failed to stringify param1. type=" + raw.getClass().getName()
          + " err=" + e.getMessage());
      return null;
    }

    if (json == null || json.isBlank()) {
      System.out.println("❌ BUILDER_STATS: json is blank");
      return null;
    }

    List<BuilderStatTileDto> tiles;
    try {
      tiles = objectMapper.readValue(json, new TypeReference<List<BuilderStatTileDto>>() {});
    } catch (Exception e) {
      System.out.println("❌ BUILDER_STATS: JSON parse failed. json=" + json + " err=" + e.getMessage());
      return null;
    }

    if (tiles == null) tiles = Collections.emptyList();

    tiles = tiles.stream()
        .filter(t -> t != null
            && t.getValue() != null && !t.getValue().isBlank()
            && t.getLabel() != null && !t.getLabel().isBlank())
        .toList();

    System.out.println("✅ BUILDER_STATS: tiles.size=" + tiles.size());

    if (tiles.isEmpty()) return null;

    return HomeSectionDto.<BuilderStatTileDto>builder()
        .key("STATS")
        .type(HomeSectionType.BUILDER_STATS)
        .title(cfg.getTitle())
        .items(tiles)
        .build();
  }
}