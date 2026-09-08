package com.brandPitara.sfs.projectcompare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparisonInsightBlockResponse {
    private final String key;
    private final String icon;
    private final String heading;
    private final String body;
    private final Long winnerProjectId;
    private final String winnerProjectName;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ComparisonInsightMetricResponse> metrics;
}
