package com.brandPitara.sfs.project.enums;

public enum InsightComparisonResult {
    BETTER,
    AVERAGE,
    BELOW_AVERAGE;

    public String toLabel() {
        return switch (this) {
            case BETTER        -> "Better than Average";
            case AVERAGE       -> "Average";
            case BELOW_AVERAGE -> "Below Average";
        };
    }
}
