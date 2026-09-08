package com.brandPitara.sfs.analytics.dto;

import lombok.Builder;
import lombok.Getter;

/** Deliberately minimal - a batch endpoint must not return a heavy per-event payload. */
@Getter
@Builder
public class AnalyticsIngestResponse {
    private int accepted;
    private int rejected;
}
