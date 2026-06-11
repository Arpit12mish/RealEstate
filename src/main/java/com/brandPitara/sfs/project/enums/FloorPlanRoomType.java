package com.brandPitara.sfs.project.enums;

public enum FloorPlanRoomType {
    LIVING_ROOM,
    DINING_ROOM,
    KITCHEN,
    MASTER_BEDROOM,
    BEDROOM,
    BATHROOM,
    BALCONY,
    STUDY_ROOM,
    SERVANT_ROOM,
    UTILITY_ROOM,
    POOJA_ROOM,
    FOYER,
    STORE_ROOM,
    TERRACE,
    GARAGE,
    OTHER;

    public String toLabel() {
        return switch (this) {
            case LIVING_ROOM    -> "Living Room";
            case DINING_ROOM    -> "Dining Room";
            case KITCHEN        -> "Kitchen";
            case MASTER_BEDROOM -> "Master Bedroom";
            case BEDROOM        -> "Bedroom";
            case BATHROOM       -> "Bathroom";
            case BALCONY        -> "Balcony";
            case STUDY_ROOM     -> "Study Room";
            case SERVANT_ROOM   -> "Servant Room";
            case UTILITY_ROOM   -> "Utility Room";
            case POOJA_ROOM     -> "Pooja Room";
            case FOYER          -> "Foyer";
            case STORE_ROOM     -> "Store Room";
            case TERRACE        -> "Terrace";
            case GARAGE         -> "Garage";
            case OTHER          -> "Other";
        };
    }
}
