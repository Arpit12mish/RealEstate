package com.brandPitara.sfs.buildercredibility.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderCredibilityMetricDto {
    private String key;
    private String label;
    private String value;
    private String subtitle;
}