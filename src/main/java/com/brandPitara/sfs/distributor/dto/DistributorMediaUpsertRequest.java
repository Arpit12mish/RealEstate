package com.brandPitara.sfs.distributor.dto;

import com.brandPitara.sfs.distributor.entity.DistributorMediaEntity.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DistributorMediaUpsertRequest {

  @NotNull
  private MediaType mediaType;

  @NotBlank
  private String url;

  private String caption;
  private Integer sortOrder;
  private Boolean active;
}
