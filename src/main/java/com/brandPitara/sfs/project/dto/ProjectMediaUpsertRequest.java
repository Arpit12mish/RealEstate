package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectMediaType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMediaUpsertRequest {

  @NotNull
  private ProjectMediaType mediaType;

  @NotNull
  private String url;

  private String caption;
  private Integer sortOrder;
  private Boolean active;
}
