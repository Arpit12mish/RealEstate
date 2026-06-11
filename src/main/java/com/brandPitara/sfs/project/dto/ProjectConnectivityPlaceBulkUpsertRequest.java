package com.brandPitara.sfs.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityPlaceBulkUpsertRequest {

  @Valid
  @NotEmpty
  @Size(max = 100)
  private List<ProjectConnectivityPlaceUpsertRequest> places;
}
