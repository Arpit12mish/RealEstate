package com.brandPitara.sfs.dashboard.review.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkFieldIssueFixedRequest {

    @Size(max = 2000)
    private String remarks;
}