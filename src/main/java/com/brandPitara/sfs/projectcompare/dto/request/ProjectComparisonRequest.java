package com.brandPitara.sfs.projectcompare.dto.request;

import com.brandPitara.sfs.projectcompare.enums.ComparisonSectionKey;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProjectComparisonRequest {

    @NotEmpty(message = "projectIds must not be empty")
    @Size(min = 2, max = 4, message = "Provide between 2 and 4 project IDs")
    private List<@NotNull Long> projectIds;

    /** Optional; when absent all sections are returned. */
    private List<ComparisonSectionKey> sectionKeys;
}
