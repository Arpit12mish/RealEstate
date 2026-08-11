package com.brandPitara.sfs.company.enums;

import java.util.Optional;
import java.util.Set;

/**
 * Single authoritative mapping from the public, normalized Architect/Interior
 * Designer domain concept to the underlying free-text {@code Company.companyType}
 * storage values that represent it. {@code companyType} has no enum/CHECK
 * constraint at the database level (confirmed via {@code CompanyEntity} - plain
 * {@code VARCHAR(40)}), and today holds five distinct dashboard-authored string
 * variants across these two domain concepts - this enum exists specifically so
 * that duplication of those five literal strings (previously independently
 * hardcoded in {@code ArchitectsSectionLoader}, {@code DesignersSectionLoader},
 * and {@code ArchitectsAndDesignersSectionLoader}) has exactly one source of
 * truth going forward (Phase 8A-G, closing GAP-037).
 */
public enum ArchitectDesignerType {
  ARCHITECT(Set.of("ARCHITECT", "ARCHITECT&DESIGNERS")),
  INTERIOR_DESIGNER(Set.of("DESIGNER", "DESIGNERS", "INTERIOR_DESIGNER"));

  private final Set<String> storageValues;

  ArchitectDesignerType(Set<String> storageValues) {
    this.storageValues = storageValues;
  }

  /** The exact {@code Company.companyType} string values this public type represents. */
  public Set<String> storageValues() {
    return storageValues;
  }

  /**
   * Parses a public {@code type} request parameter (case-insensitive) into its
   * normalized enum value. Returns empty - never throws - for {@code null},
   * blank, or any unrecognized value; callers decide how to fail (this
   * codebase has no safe handler for a raw Spring enum-binding failure on a
   * public endpoint, the same root cause as GAP-032, so binding this
   * parameter as a plain String and parsing explicitly here is deliberate).
   */
  public static Optional<ArchitectDesignerType> parse(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String normalized = raw.trim().toUpperCase();
    for (ArchitectDesignerType type : values()) {
      if (type.name().equals(normalized)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
