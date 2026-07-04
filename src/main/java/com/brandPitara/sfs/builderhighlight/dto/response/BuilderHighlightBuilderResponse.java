package com.brandPitara.sfs.builderhighlight.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightBuilderResponse {
    private Long id;
    private String name;
    private String logoUrl;
    private Integer credibilityScore;
    private String credibilityLabel;
}
