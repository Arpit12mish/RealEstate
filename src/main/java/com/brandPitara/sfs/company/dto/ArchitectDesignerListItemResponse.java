package com.brandPitara.sfs.company.dto;

import lombok.*;

/**
 * Public listing item for {@code GET /api/public/architect-designers?type=...}
 * (Phase 8A-G, closing GAP-037). Deliberately not {@link ArchitectDesignerCardDto}
 * (the Home-feed card - carries synthesized per-card stats built from a
 * separate query, never a public API contract) and not {@link CompanyResponse}
 * (the generic Company DTO - carries {@code phone}/{@code whatsapp}, which
 * neither this dedicated Architect/Designer contract nor mobile's own
 * architect/designer screens have ever exposed). Contains only fields
 * directly present on {@code CompanyEntity} and safe to list publicly -
 * no per-row child query (stats/awards/certificates/projects) is issued to
 * populate this DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectDesignerListItemResponse {
  private Long companyId;
  private String slug;
  private String name;

  /** Normalized {@code "ARCHITECT"} / {@code "INTERIOR_DESIGNER"} - the queried type, never the raw {@code companyType} storage value. */
  private String type;

  private String logoUrl;
  private String coverImageUrl;
  private String description;
}
