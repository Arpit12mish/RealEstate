package com.brandPitara.sfs.brand.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandFaqUpsertRequest {

  @NotBlank
  @Size(max = 300)
  private String question;

  @NotBlank
  @Size(max = 4000)
  private String answer;

  @Min(0) @Max(9999)
  private Integer displayOrder;

  private Boolean active;
}
