package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;

import java.util.List;

public final class ProjectMediaPicker {

  private ProjectMediaPicker() {}

  public static Picked pick(List<ProjectMediaEntity> media) {
    if (media == null || media.isEmpty()) return Picked.empty();

    boolean hasImages = media.stream().anyMatch(m -> m.getMediaType() == ProjectMediaType.IMAGE);
    boolean hasVideo  = media.stream().anyMatch(m -> m.getMediaType() == ProjectMediaType.VIDEO);

    String brochureUrl = media.stream()
        .filter(m -> m.getMediaType() == ProjectMediaType.BROCHURE_PDF)
        .map(ProjectMediaEntity::getUrl)
        .findFirst()
        .orElse(null);

    ProjectMediaEntity cover = media.stream()
        .filter(m -> m.getMediaType() == ProjectMediaType.IMAGE)
        .findFirst()
        .orElseGet(() -> media.stream()
            .filter(m -> m.getMediaType() == ProjectMediaType.VIDEO)
            .findFirst()
            .orElse(null));

    String coverUrl = cover != null ? cover.getUrl() : null;
    String coverType = cover != null ? cover.getMediaType().name() : null;

    return new Picked(coverUrl, coverType, brochureUrl, hasImages, hasVideo);
  }

  public record Picked(
      String coverMediaUrl,
      String coverMediaType,
      String brochureUrl,
      boolean hasImages,
      boolean hasVideo
  ) {
    public static Picked empty() {
      return new Picked(null, null, null, false, false);
    }
  }
}
