package com.brandPitara.sfs.builderhighlight.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightsScreenResponse {
    private BuilderHighlightBuilderResponse builder;
    private List<BuilderHighlightSectionResponse> sections;
}
