package com.brandPitara.sfs.builderhighlight.dto.response;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightPointType;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightPointResponse {
    private Long id;
    private BuilderHighlightPointType pointType;
    private String title;
    private String text;
    private String iconKey;
    private Integer displayOrder;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
