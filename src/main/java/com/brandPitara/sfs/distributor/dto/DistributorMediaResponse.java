package com.brandPitara.sfs.distributor.dto;

import com.brandPitara.sfs.distributor.entity.DistributorMediaEntity.MediaType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DistributorMediaResponse {
  private Long id;
  private MediaType mediaType;
  private String url;
  private String caption;
  private int sortOrder;
  private boolean active;
}
