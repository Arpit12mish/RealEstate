package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanVisualAnalysisUpsertRequest {

  @Size(max = 160)
  private String title;

  @Size(max = 500)
  private String description;

  private FloorPlanVisualMediaType mediaType;

  // Format/host validation (GAP-028) happens in TrustedMediaUrlValidator,
  // called explicitly from ProjectFloorPlanVisualAnalysisServiceImpl#upsert -
  // not expressible as a single Bean Validation annotation (needs URI
  // parsing + a configuration-driven host allowlist). This @Size bound is a
  // plain sanity ceiling on top of that, consistent with this codebase's own
  // "bound every string field" discipline - the underlying column is TEXT
  // (unbounded), so this is a defensive API-layer limit, not a DB constraint.
  @Size(max = 2048)
  private String mediaUrl;

  @Valid
  @Size(max = 20)
  private List<VisualAnalysisTagUpsertRequest> tags;

  private Boolean active;
}
