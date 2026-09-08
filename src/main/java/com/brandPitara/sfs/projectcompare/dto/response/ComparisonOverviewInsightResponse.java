package com.brandPitara.sfs.projectcompare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparisonOverviewInsightResponse {
    private final String title;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ComparisonInsightBlockResponse> blocks;

    private final ComparisonVerdictResponse verdict;
}
