package com.brandPitara.sfs.builderhighlight.dto.response;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightPointType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightPublicPointResponse {
    private Long id;
    private BuilderHighlightPointType pointType;
    private String title;
    private String text;
    private String iconKey;
    private Integer displayOrder;
    private Boolean active;
}
