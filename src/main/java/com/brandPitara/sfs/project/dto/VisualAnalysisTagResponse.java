package com.brandPitara.sfs.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisualAnalysisTagResponse {
  private String label;
  private String color;
  private int sortOrder;
}
