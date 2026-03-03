// com.brandPitara.sfs.home.dto.HomeSectionDto.java
package com.brandPitara.sfs.home.dto;

import lombok.*;

import java.util.List;

import com.brandPitara.sfs.home.enums.HomeSectionType;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HomeSectionDto<T> {
  private HomeSectionType type;
  private String key;           // ✅ new future-proof key (ABOUT, REVIEWS, ...)
  private String title;
  private List<T> items;
}
