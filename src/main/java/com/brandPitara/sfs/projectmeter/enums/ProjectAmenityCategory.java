package com.brandPitara.sfs.projectmeter.enums;

public enum ProjectAmenityCategory {
    LIFESTYLE,
    SPORTS,
    NATURAL,
    WELLNESS,
    SECURITY,
    CONVENIENCE,
    COMMUNITY,
    PARKING_TRANSPORT,
    GREEN_SUSTAINABILITY,
    KIDS_FAMILY,
    PET_FRIENDLY,
    COMMERCIAL_RETAIL,
    OTHER;

    public String toLabel() {
        return switch (this) {
            case LIFESTYLE            -> "Lifestyle Amenities";
            case SPORTS               -> "Sports Amenities";
            case NATURAL              -> "Natural Amenities";
            case WELLNESS             -> "Wellness Amenities";
            case SECURITY             -> "Security & Safety";
            case CONVENIENCE          -> "Convenience Amenities";
            case COMMUNITY            -> "Community Spaces";
            case PARKING_TRANSPORT    -> "Parking & Transport";
            case GREEN_SUSTAINABILITY -> "Green & Sustainability";
            case KIDS_FAMILY          -> "Kids & Family";
            case PET_FRIENDLY         -> "Pet Friendly";
            case COMMERCIAL_RETAIL    -> "Commercial / Retail";
            case OTHER                -> "Other Amenities";
        };
    }
}
