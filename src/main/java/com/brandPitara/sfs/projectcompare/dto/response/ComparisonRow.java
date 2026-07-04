package com.brandPitara.sfs.projectcompare.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ComparisonRow {
    /** Stable identifier used by frontend to look up the row (e.g. "name", "priceRange"). */
    private final String rowKey;

    /** Human-readable label rendered in the first column (e.g. "Price Range"). */
    private final String label;

    /**
     * Hint for frontend rendering: TEXT, CURRENCY, PERCENT, SCORE, DATE, BADGE, BOOLEAN.
     * Frontend uses this to apply optional formatting or icons — never for business logic.
     */
    private final String valueType;

    /** When true the row is visually highlighted (e.g. Meter Score). */
    private final boolean highlight;

    /** Key = projectId (as String), value = cell data for that project. */
    private final Map<String, ComparisonCellValue> values;
}
