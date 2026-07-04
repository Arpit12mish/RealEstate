package com.brandPitara.sfs.projectcompare.enums;

public enum ComparisonSectionKey {
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
            case OVERVIEW     -> 1;
            case PRICE        -> 2;
            case UNITS        -> 3;
            case AMENITIES    -> 4;
            case LOCATION     -> 5;
            case CONSTRUCTION -> 6;
            case COMPLIANCE   -> 7;
            case BUILDER      -> 8;
            case METER        -> 9;
        };
    }

    public boolean initiallyExpanded() {
        return this == OVERVIEW || this == PRICE;
    }
}
