package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.entity.BrandMediaEntity.MediaType;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandMediaResponse {
  private Long id;
  private MediaType mediaType;
  private Placement placement;
  private String url;
  private String caption;
  private int sortOrder;

  private String actionType;
  private String actionValue;
}
