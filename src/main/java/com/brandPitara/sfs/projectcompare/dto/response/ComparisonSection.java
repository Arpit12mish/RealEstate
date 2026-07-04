package com.brandPitara.sfs.projectcompare.dto.response;

import com.brandPitara.sfs.projectcompare.enums.ComparisonSectionKey;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComparisonSection {
    private final ComparisonSectionKey sectionKey;
    private final String sectionTitle;
    private final int displayOrder;
    private final boolean initiallyExpanded;
    /** Optional note about the section (e.g. "All compared projects are by M3M"). Omitted from JSON when null. */
    private final String description;
    private final List<ComparisonRow> rows;
}
