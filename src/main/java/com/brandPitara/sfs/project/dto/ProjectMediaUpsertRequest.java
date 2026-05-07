package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectMediaType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMediaUpsertRequest {

  @NotNull
  private ProjectMediaType mediaType;

  @NotBlank
  @Size(max = 500)
  private String url;

  @Size(max = 300)
  private String caption;

  @Min(0) @Max(9999)
  private Integer sortOrder;
  private Boolean active;
}
