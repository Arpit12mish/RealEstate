package com.brandPitara.sfs.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectMasterPlanStatResponse {
  private String key;
  private String label;
  private String value;
  private Object rawValue;
  private String unit;
  private Integer displayOrder;
}
