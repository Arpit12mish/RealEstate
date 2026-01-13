package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectMediaType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMediaResponse {
  private Long id;
  private Long projectId;
  private ProjectMediaType mediaType;
  private String url;
  private String caption;
  private int sortOrder;
  private boolean active;
}
