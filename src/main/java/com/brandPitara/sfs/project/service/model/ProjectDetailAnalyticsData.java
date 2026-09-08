package com.brandPitara.sfs.project.service.model;

import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;

/** Detached Meter-domain values that Project Detail actually consumes. */
public record ProjectDetailAnalyticsData(
        Long snapshotAverageAreaPrice,
        Double snapshotAppreciationPercent,
        ProjectAmenitiesResponse amenities
) {
}
