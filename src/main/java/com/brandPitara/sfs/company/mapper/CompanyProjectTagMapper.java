package com.brandPitara.sfs.company.mapper;

import java.util.Arrays;
import java.util.List;

public final class CompanyProjectTagMapper {

  private CompanyProjectTagMapper() {
  }

  public static List<String> toTags(String raw) {
    if (raw == null || raw.isBlank()) return List.of();

    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .distinct()
        .toList();
  }
}