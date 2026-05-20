package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ApplyMode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApplyToProjectRequest {

    @NotNull(message = "mode must not be null.")
    private ApplyMode mode;

    // Required when mode=CREATE_NEW_PROJECT (or when candidate has no linkedBuilderId).
    private Long builderId;

    // Required when mode=UPDATE_EXISTING_PROJECT.
    private Long projectId;

    // Optional city mapping — candidate only stores cityName text; admin must supply cityId.
    private Long cityId;

    // When false (default), existing non-null project fields are preserved.
    // When true, candidate values overwrite existing project fields.
    private boolean overwrite = false;
}
