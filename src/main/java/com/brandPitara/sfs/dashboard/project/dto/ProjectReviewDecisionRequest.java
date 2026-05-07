package com.brandPitara.sfs.dashboard.project.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectReviewDecisionRequest {

    @Size(max = 2000)
    private String remarks;
}