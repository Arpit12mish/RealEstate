package com.brandPitara.sfs.project.enums;

public enum UnitConfigurationType {
    STUDIO,
    BHK_1,
    BHK_1_5,
    BHK_2,
    BHK_2_5,
    BHK_3,
    BHK_3_5,
    BHK_4,
    BHK_4_5,
    BHK_5,
    BHK_5_PLUS,
    OFFICE,
    RETAIL,
    PLOT,
    OTHER;

    public String toLabel() {
        return switch (this) {
            case STUDIO    -> "Studio";
            case BHK_1     -> "1 BHK";
            case BHK_1_5   -> "1.5 BHK";
            case BHK_2     -> "2 BHK";
            case BHK_2_5   -> "2.5 BHK";
            case BHK_3     -> "3 BHK";
            case BHK_3_5   -> "3.5 BHK";
            case BHK_4     -> "4 BHK";
            case BHK_4_5   -> "4.5 BHK";
            case BHK_5     -> "5 BHK";
            case BHK_5_PLUS -> "5+ BHK";
            case OFFICE    -> "Office";
            case RETAIL    -> "Retail";
            case PLOT      -> "Plot";
            case OTHER     -> "Other";
        };
    }
}
