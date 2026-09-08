package com.brandPitara.sfs.analytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AnalyticsEventBatchRequest {

    @NotEmpty
    @Size(max = 50, message = "A batch may contain at most 50 events")
    @Valid
    private List<AnalyticsEventRequest> events;
}
