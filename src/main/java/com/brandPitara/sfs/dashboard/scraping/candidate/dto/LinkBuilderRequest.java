package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkBuilderRequest {

    @NotNull(message = "builderId must not be null.")
    private Long builderId;
}
