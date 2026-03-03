package com.brandPitara.sfs.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryCardDto {
  private Long id;
  private String name;
  private String slug;
}
