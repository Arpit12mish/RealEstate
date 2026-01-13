package com.brandPitara.sfs.builder.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BuilderCardResponse {
  private Long id;
  private String name;
  private String logoUrl;
  private int priority;
}
