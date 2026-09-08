package com.brandPitara.sfs.projectcompare.enums;

public enum ComparisonSectionKey {
    VISUAL_COMPARISON,
    OVERVIEW,
    PRICE,
    UNITS,
    AMENITIES,
    LOCATION,
    CONSTRUCTION,
    COMPLIANCE,
    BUILDER,
    METER;

    public String toTitle() {
        return switch (this) {
            case VISUAL_COMPARISON -> "Visual Comparison";
            case OVERVIEW     -> "Overview";
            case PRICE        -> "Price & Insights";
            case UNITS        -> "Units & Floor Plans";
            case AMENITIES    -> "Amenities";
            case LOCATION     -> "Location & Connectivity";
            case CONSTRUCTION -> "Construction";
            case COMPLIANCE   -> "Approvals & Compliance";
            case BUILDER      -> "Builder Credibility";
            case METER        -> "Meter Score";
        };
    }

    public int defaultOrder() {
        return switch (this) {
            case VISUAL_COMPARISON -> 1;
            case OVERVIEW     -> 2;
            case PRICE        -> 3;
            case UNITS        -> 4;
            case AMENITIES    -> 5;
            case LOCATION     -> 6;
            case CONSTRUCTION -> 7;
            case COMPLIANCE   -> 8;
            case BUILDER      -> 9;
            case METER        -> 10;
        };
    }

    public boolean initiallyExpanded() {
        return this == VISUAL_COMPARISON || this == OVERVIEW || this == PRICE;
    }
}
