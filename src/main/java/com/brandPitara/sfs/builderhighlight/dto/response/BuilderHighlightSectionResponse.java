package com.brandPitara.sfs.builderhighlight.dto.response;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightSectionResponse {
    private BuilderHighlightType type;
    private String title;
    private String subtitle;
    private List<BuilderHighlightPublicItemResponse> items;
}
