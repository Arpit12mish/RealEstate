package com.brandPitara.sfs.brand.dto;

import lombok.*;

import java.util.List;

/**
 * Shared response envelope for both the project and builder "connected brands" sections.
 * Only the field matching the caller's context is populated (projectId for the project
 * endpoint, builderId for the builder endpoint) - the other stays null.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicConnectedBrandsSectionResponse {
  private Long projectId;
  private Long builderId;
  private List<PublicBrandConnectedResponse> items;
}
