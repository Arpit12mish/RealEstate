package com.brandPitara.sfs.projectcompare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparisonVerdictResponse {
    private final Long winnerProjectId;
    private final String winnerProjectName;
    private final String heading;
    private final String body;
    private final String tone;
}
