package com.brandPitara.sfs.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectHighlightResponse {
  private Long id;
  private Long projectId;
  private String title;
  private String subtitle;
  private String iconKey;
  private int sortOrder;
  private boolean active;
}