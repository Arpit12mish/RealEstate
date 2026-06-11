package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementBuilderResponseDto {

    private String title;
    private String personName;
    private String personDesignation;
    private String responseText;
}