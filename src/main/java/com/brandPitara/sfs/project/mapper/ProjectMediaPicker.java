package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ProjectMediaPicker {

  private ProjectMediaPicker() {}

  /**
   * Strict pick:
   * - If media indicates images/videos exist but cover can't be resolved, throws IllegalStateException.
   */
  public static Picked pick(List<ProjectMediaEntity> media) {
    return pick(media, true);
  }

  /**
   * @param strict if true: throws when cover should exist but can't be picked.
   *               if false: returns null cover fields safely.
   */
  public static Picked pick(List<ProjectMediaEntity> media, boolean strict) {
    if (media == null || media.isEmpty()) return Picked.empty();

    // 1) Filter to valid rows only
    List<ProjectMediaEntity> list = media.stream()
        .filter(Objects::nonNull)
        .filter(m -> Boolean.TRUE.equals(m.getActive()))
        .filter(m -> Boolean.FALSE.equals(m.getDeleted()))
        .filter(m -> m.getMediaType() != null)
        .filter(m -> m.getUrl() != null && !m.getUrl().isBlank())
        .toList();

    if (list.isEmpty()) return Picked.empty();

    // 2) Deterministic ordering: cover should be stable
    Comparator<ProjectMediaEntity> stableOrder =
        Comparator.comparing(ProjectMediaEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(ProjectMediaEntity::getId, Comparator.nullsLast(Comparator.reverseOrder()));

    boolean hasImages = list.stream().anyMatch(m -> m.getMediaType() == ProjectMediaType.IMAGE);
    boolean hasVideo  = list.stream().anyMatch(m -> m.getMediaType() == ProjectMediaType.VIDEO);

    // 3) brochure: first BROCHURE_PDF by stable order
    String brochureUrl = list.stream()
        .filter(m -> m.getMediaType() == ProjectMediaType.BROCHURE_PDF)
        .sorted(stableOrder)
        .map(ProjectMediaEntity::getUrl)
        .findFirst()
        .orElse(null);

    // 4) cover: prefer IMAGE first, else VIDEO (both sorted)
    ProjectMediaEntity cover = list.stream()
        .filter(m -> m.getMediaType() == ProjectMediaType.IMAGE)
        .sorted(stableOrder)
        .findFirst()
        .orElseGet(() -> list.stream()
            .filter(m -> m.getMediaType() == ProjectMediaType.VIDEO)
            .sorted(stableOrder)
            .findFirst()
            .orElse(null));

    // 5) Strict failure if we "should" have a cover but can't pick it
    if (cover == null && (hasImages || hasVideo) && strict) {
      // Helpful debug: what types did we actually receive?
      String types = list.stream()
          .map(m -> m.getMediaType().name())
          .distinct()
          .sorted()
          .reduce((a, b) -> a + "," + b)
          .orElse("NONE");

      throw new IllegalStateException(
          "ProjectMediaPicker: cover media expected but not resolved. " +
              "hasImages=" + hasImages + ", hasVideo=" + hasVideo +
              ", receivedTypes=" + types +
              ". Check active/deleted/url/sortOrder and repository query."
      );
    }

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
