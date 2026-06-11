package com.brandPitara.sfs.project.enums;

public enum FloorPlanInsightType {
    // Room-size comparisons
    MASTER_BEDROOM_SIZE,
    CHILD_BEDROOM_SIZE,
    LIVING_ROOM_SIZE,
    DINING_ROOM_SIZE,
    KITCHEN_SIZE,
    BALCONY_SIZE,

    // Area efficiency
    CARPET_AREA_EFFICIENCY,

    // Ventilation & light
    NATURAL_VENTILATION,
    CROSS_VENTILATION,
    NATURAL_LIGHT,

    // Layout / comfort
    VASTU_COMPLIANCE,
    PRIVACY_SCORE,
    FLOW_EFFICIENCY,
    SPACE_UTILIZATION,

    OTHER;

    public String toLabel() {
        return switch (this) {
            case MASTER_BEDROOM_SIZE    -> "Master Bedroom Size";
            case CHILD_BEDROOM_SIZE     -> "Child Bedroom Size";
            case LIVING_ROOM_SIZE       -> "Living Room Size";
            case DINING_ROOM_SIZE       -> "Dining Room Size";
            case KITCHEN_SIZE           -> "Kitchen Size";
            case BALCONY_SIZE           -> "Balcony Size";
            case CARPET_AREA_EFFICIENCY -> "Carpet Area Efficiency";
            case NATURAL_VENTILATION    -> "Natural Ventilation";
            case CROSS_VENTILATION      -> "Cross Ventilation";
            case NATURAL_LIGHT          -> "Natural Light";
            case VASTU_COMPLIANCE       -> "Vastu Compliance";
            case PRIVACY_SCORE          -> "Privacy Score";
            case FLOW_EFFICIENCY        -> "Flow Efficiency";
            case SPACE_UTILIZATION      -> "Space Utilization";
            case OTHER                  -> "Other";
        };
    }
}
