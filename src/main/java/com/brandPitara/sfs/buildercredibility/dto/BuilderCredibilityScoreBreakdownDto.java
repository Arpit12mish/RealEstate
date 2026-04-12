package com.brandPitara.sfs.buildercredibility.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderCredibilityScoreBreakdownDto {
    private String key;
    private String label;
    private Integer score;
    private Integer maxScore;
    private String summary;
}