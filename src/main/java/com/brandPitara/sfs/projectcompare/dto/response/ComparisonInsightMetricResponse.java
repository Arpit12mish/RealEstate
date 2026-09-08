package com.brandPitara.sfs.projectcompare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparisonInsightMetricResponse {
    private final String label;
    private final Map<String, String> projectValues;
}
