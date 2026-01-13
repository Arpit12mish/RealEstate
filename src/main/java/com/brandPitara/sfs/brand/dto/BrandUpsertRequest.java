package com.brandPitara.sfs.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  private String logoUrl;
  private String description;

  private Integer priority;
  private Boolean active;
}
