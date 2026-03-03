// com.brandPitara.sfs.home.dto.HomeFeedResponse.java
package com.brandPitara.sfs.home.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HomeFeedResponse {
  private long version;
  private HomeHeaderDto header;
  private List<HomeSectionDto<?>> sections;
}

