package com.brandPitara.sfs.projectcompare.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProjectComparisonResponse {
    /** Column headers in the order requested by the client. */
    private final List<ComparisonProjectHeader> projects;

    /** Sections in ascending displayOrder; each section contains its rows. */
    private final List<ComparisonSection> sections;
}
