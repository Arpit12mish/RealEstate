package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.project.dto.AmenityCategoryMetaResponse;
import com.brandPitara.sfs.dashboard.project.dto.AmenitySuggestionResponse;
import com.brandPitara.sfs.dashboard.project.dto.PropertyTypeMetaResponse;
import com.brandPitara.sfs.dashboard.project.dto.UnitConfigurationMetaResponse;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/project-metadata")
public class DashboardProjectMetadataController {

    @GetMapping("/property-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<PropertyTypeMetaResponse> getPropertyTypes() {
        return ALL_PROPERTY_TYPES;
    }

    private static final List<PropertyTypeMetaResponse> ALL_PROPERTY_TYPES = List.of(
        // Residential
        meta(PropertyType.RESIDENTIAL,       "Residential",       "Residential"),
        meta(PropertyType.APARTMENT,         "Apartment",         "Residential"),
        meta(PropertyType.STUDIO,            "Studio",            "Residential"),
        meta(PropertyType.VILLA,             "Villa",             "Residential"),
        meta(PropertyType.INDEPENDENT_HOUSE, "Independent House", "Residential"),
        meta(PropertyType.BUILDER_FLOOR,     "Builder Floor",     "Residential"),
        meta(PropertyType.ROW_HOUSE,         "Row House",         "Residential"),
        meta(PropertyType.PENTHOUSE,         "Penthouse",         "Residential"),
        meta(PropertyType.DUPLEX,            "Duplex",            "Residential"),
        meta(PropertyType.FARMHOUSE,         "Farmhouse",         "Residential"),

        // Land / Plot
        meta(PropertyType.PLOT,              "Plot",              "Land / Plot"),
        meta(PropertyType.RESIDENTIAL_PLOT,  "Residential Plot",  "Land / Plot"),
        meta(PropertyType.COMMERCIAL_PLOT,   "Commercial Plot",   "Land / Plot"),
        meta(PropertyType.AGRICULTURAL_LAND, "Agricultural Land", "Land / Plot"),

        // Commercial
        meta(PropertyType.COMMERCIAL,        "Commercial",        "Commercial"),
        meta(PropertyType.OFFICE_SPACE,      "Office Space",      "Commercial"),
        meta(PropertyType.RETAIL_SHOP,       "Retail Shop",       "Commercial"),
        meta(PropertyType.SHOWROOM,          "Showroom",          "Commercial"),
        meta(PropertyType.FOOD_COURT,        "Food Court",        "Commercial"),
        meta(PropertyType.CO_WORKING_SPACE,  "Co-working Space",  "Commercial"),

        // Mixed Use / Hospitality
        meta(PropertyType.MIXED_USE,         "Mixed Use",         "Mixed Use / Hospitality"),
        meta(PropertyType.SERVICED_APARTMENT,"Serviced Apartment","Mixed Use / Hospitality"),
        meta(PropertyType.HOTEL,             "Hotel",             "Mixed Use / Hospitality"),
        meta(PropertyType.RESORT,            "Resort",            "Mixed Use / Hospitality"),

        // Industrial / Institutional
        meta(PropertyType.INDUSTRIAL,        "Industrial",        "Industrial / Institutional"),
        meta(PropertyType.WAREHOUSE,         "Warehouse",         "Industrial / Institutional"),
        meta(PropertyType.FACTORY,           "Factory",           "Industrial / Institutional"),
        meta(PropertyType.INSTITUTIONAL,     "Institutional",     "Industrial / Institutional"),

        // Other
        meta(PropertyType.OTHER,             "Other",             "Other")
    );

    @GetMapping("/unit-configurations")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<UnitConfigurationMetaResponse> getUnitConfigurations() {
        return ALL_UNIT_CONFIGURATIONS;
    }

    private static final List<UnitConfigurationMetaResponse> ALL_UNIT_CONFIGURATIONS = List.of(
        // Residential
        unitMeta(UnitConfigurationType.STUDIO,    "Studio",    "Residential"),
        unitMeta(UnitConfigurationType.BHK_1,     "1 BHK",     "Residential"),
        unitMeta(UnitConfigurationType.BHK_1_5,   "1.5 BHK",   "Residential"),
        unitMeta(UnitConfigurationType.BHK_2,     "2 BHK",     "Residential"),
        unitMeta(UnitConfigurationType.BHK_2_5,   "2.5 BHK",   "Residential"),
        unitMeta(UnitConfigurationType.BHK_3,     "3 BHK",     "Residential"),
        unitMeta(UnitConfigurationType.BHK_3_5,   "3.5 BHK",   "Residential"),
        unitMeta(UnitConfigurationType.BHK_4,     "4 BHK",     "Residential"),
        unitMeta(UnitConfigurationType.BHK_4_5,   "4.5 BHK",   "Residential"),
        unitMeta(UnitConfigurationType.BHK_5,     "5 BHK",     "Residential"),
        unitMeta(UnitConfigurationType.BHK_5_PLUS,"5+ BHK",    "Residential"),

        // Commercial
        unitMeta(UnitConfigurationType.OFFICE,    "Office",    "Commercial"),
        unitMeta(UnitConfigurationType.RETAIL,    "Retail",    "Commercial"),

        // Land
        unitMeta(UnitConfigurationType.PLOT,      "Plot",      "Land"),

        // Other
        unitMeta(UnitConfigurationType.OTHER,     "Other",     "Other")
    );

    private static PropertyTypeMetaResponse meta(PropertyType value, String label, String group) {
        return new PropertyTypeMetaResponse(value, label, group);
    }

    private static UnitConfigurationMetaResponse unitMeta(UnitConfigurationType value, String label, String group) {
        return new UnitConfigurationMetaResponse(value, label, group);
    }

    // ── Amenity Categories ─────────────────────────────────────────────────────

    @GetMapping("/amenity-categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<AmenityCategoryMetaResponse> getAmenityCategories() {
        return ALL_AMENITY_CATEGORIES;
    }

    private static final List<AmenityCategoryMetaResponse> ALL_AMENITY_CATEGORIES = List.of(
        amenityCat(ProjectAmenityCategory.LIFESTYLE,           "Lifestyle Amenities",    1),
        amenityCat(ProjectAmenityCategory.SPORTS,              "Sports Amenities",        2),
        amenityCat(ProjectAmenityCategory.WELLNESS,            "Wellness Amenities",      3),
        amenityCat(ProjectAmenityCategory.SECURITY,            "Security & Safety",       4),
        amenityCat(ProjectAmenityCategory.CONVENIENCE,         "Convenience Amenities",   5),
        amenityCat(ProjectAmenityCategory.COMMUNITY,           "Community Spaces",        6),
        amenityCat(ProjectAmenityCategory.KIDS_FAMILY,         "Kids & Family",           7),
        amenityCat(ProjectAmenityCategory.GREEN_SUSTAINABILITY,"Green & Sustainability",  8),
        amenityCat(ProjectAmenityCategory.NATURAL,             "Natural Amenities",       9),
        amenityCat(ProjectAmenityCategory.PARKING_TRANSPORT,   "Parking & Transport",    10),
        amenityCat(ProjectAmenityCategory.PET_FRIENDLY,        "Pet Friendly",           11),
        amenityCat(ProjectAmenityCategory.COMMERCIAL_RETAIL,   "Commercial / Retail",    12),
        amenityCat(ProjectAmenityCategory.OTHER,               "Other Amenities",        99)
    );

    // ── Amenity Suggestions ────────────────────────────────────────────────────

    @GetMapping("/amenity-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<AmenitySuggestionResponse> getAmenitySuggestions() {
        return ALL_AMENITY_SUGGESTIONS;
    }

    private static final List<AmenitySuggestionResponse> ALL_AMENITY_SUGGESTIONS = List.of(
        // LIFESTYLE
        sug("swimming_pool",       "Swimming Pool",                ProjectAmenityCategory.LIFESTYLE,           "pool",         false),
        sug("clubhouse",           "Clubhouse",                    ProjectAmenityCategory.LIFESTYLE,           "clubhouse",    false),
        sug("banquet_hall",        "Banquet Hall",                 ProjectAmenityCategory.LIFESTYLE,           "banquet",      false),
        sug("rooftop_lounge",      "Rooftop Lounge",               ProjectAmenityCategory.LIFESTYLE,           "rooftop",      true),
        sug("party_lawn",          "Party Lawn",                   ProjectAmenityCategory.LIFESTYLE,           "party_lawn",   false),

        // SPORTS
        sug("gym",                 "Gymnasium",                    ProjectAmenityCategory.SPORTS,              "gym",          false),
        sug("jogging_track",       "Jogging Track",                ProjectAmenityCategory.SPORTS,              "jogging",      false),
        sug("badminton_court",     "Badminton Court",              ProjectAmenityCategory.SPORTS,              "badminton",    false),
        sug("tennis_court",        "Tennis Court",                 ProjectAmenityCategory.SPORTS,              "tennis",       false),
        sug("basketball_court",    "Basketball Court",             ProjectAmenityCategory.SPORTS,              "basketball",   false),
        sug("cricket_net",         "Cricket Nets",                 ProjectAmenityCategory.SPORTS,              "cricket",      false),
        sug("table_tennis",        "Table Tennis",                 ProjectAmenityCategory.SPORTS,              "table_tennis", false),
        sug("indoor_sports",       "Indoor Sports Area",           ProjectAmenityCategory.SPORTS,              "indoor_sports",false),
        sug("squash_court",        "Squash Court",                 ProjectAmenityCategory.SPORTS,              "squash",       true),
        sug("cycling_track",       "Cycling Track",                ProjectAmenityCategory.SPORTS,              "cycling",      true),

        // WELLNESS
        sug("yoga_deck",           "Yoga Deck",                    ProjectAmenityCategory.WELLNESS,            "yoga",         false),
        sug("meditation",          "Meditation Zone",              ProjectAmenityCategory.WELLNESS,            "meditation",   false),
        sug("spa",                 "Spa",                          ProjectAmenityCategory.WELLNESS,            "spa",          false),
        sug("steam_sauna",         "Steam & Sauna",                ProjectAmenityCategory.WELLNESS,            "steam",        true),

        // SECURITY
        sug("cctv",                "CCTV Surveillance",            ProjectAmenityCategory.SECURITY,            "cctv",         false),
        sug("gated_community",     "Gated Community",              ProjectAmenityCategory.SECURITY,            "gate",         false),
        sug("intercom",            "Video Intercom",               ProjectAmenityCategory.SECURITY,            "intercom",     false),
        sug("fire_safety",         "Fire Safety Systems",          ProjectAmenityCategory.SECURITY,            "fire",         false),
        sug("security_guard",      "24/7 Security Guard",          ProjectAmenityCategory.SECURITY,            "guard",        false),
        sug("panic_button",        "Panic Button / SOS",           ProjectAmenityCategory.SECURITY,            "panic",        true),

        // CONVENIENCE
        sug("power_backup",        "Power Backup",                 ProjectAmenityCategory.CONVENIENCE,         "power",        false),
        sug("water_supply",        "24/7 Water Supply",            ProjectAmenityCategory.CONVENIENCE,         "water",        false),
        sug("maintenance",         "Maintenance Staff",            ProjectAmenityCategory.CONVENIENCE,         "maintenance",  false),
        sug("laundry",             "Laundry Service",              ProjectAmenityCategory.CONVENIENCE,         "laundry",      false),
        sug("atm",                 "ATM",                          ProjectAmenityCategory.CONVENIENCE,         "atm",          false),
        sug("concierge",           "Concierge Service",            ProjectAmenityCategory.CONVENIENCE,         "concierge",    true),

        // COMMUNITY
        sug("multipurpose_hall",   "Multipurpose Hall",            ProjectAmenityCategory.COMMUNITY,           "multipurpose", false),
        sug("library",             "Library",                      ProjectAmenityCategory.COMMUNITY,           "library",      false),
        sug("coworking",           "Co-working Space",             ProjectAmenityCategory.COMMUNITY,           "coworking",    true),
        sug("amphitheater",        "Amphitheater",                 ProjectAmenityCategory.COMMUNITY,           "amphitheater", true),

        // KIDS_FAMILY
        sug("play_area",           "Kids' Play Area",              ProjectAmenityCategory.KIDS_FAMILY,         "play",         false),
        sug("sand_pit",            "Sand Pit",                     ProjectAmenityCategory.KIDS_FAMILY,         "sandpit",      false),
        sug("senior_sitting",      "Senior Citizen Area",          ProjectAmenityCategory.KIDS_FAMILY,         "senior",       false),
        sug("creche",              "Crèche / Daycare",             ProjectAmenityCategory.KIDS_FAMILY,         "creche",       true),

        // GREEN_SUSTAINABILITY
        sug("rainwater_harvesting","Rainwater Harvesting",         ProjectAmenityCategory.GREEN_SUSTAINABILITY,"rainwater",    false),
        sug("sewage_treatment",    "STP / Sewage Treatment",       ProjectAmenityCategory.GREEN_SUSTAINABILITY,"stp",          false),
        sug("solar_power",         "Solar Power",                  ProjectAmenityCategory.GREEN_SUSTAINABILITY,"solar",        true),
        sug("green_building",      "Green Building Certified",     ProjectAmenityCategory.GREEN_SUSTAINABILITY,"green_cert",   true),
        sug("composting",          "Composting Area",              ProjectAmenityCategory.GREEN_SUSTAINABILITY,"compost",      true),

        // NATURAL
        sug("garden",              "Garden / Landscaping",         ProjectAmenityCategory.NATURAL,             "garden",       false),
        sug("park",                "Park / Green Area",            ProjectAmenityCategory.NATURAL,             "park",         false),
        sug("lake_view",           "Lake View",                    ProjectAmenityCategory.NATURAL,             "lake",         true),
        sug("forest_trail",        "Forest Trail",                 ProjectAmenityCategory.NATURAL,             "forest",       true),
        sug("organic_farm",        "Organic Farm",                 ProjectAmenityCategory.NATURAL,             "farm",         true),

        // PARKING_TRANSPORT
        sug("covered_parking",     "Covered Parking",              ProjectAmenityCategory.PARKING_TRANSPORT,   "parking",      false),
        sug("visitor_parking",     "Visitor Parking",              ProjectAmenityCategory.PARKING_TRANSPORT,   "visitor_park", false),
        sug("cycle_parking",       "Cycle Parking",                ProjectAmenityCategory.PARKING_TRANSPORT,   "cycle_park",   false),
        sug("ev_charging",         "EV Charging Station",          ProjectAmenityCategory.PARKING_TRANSPORT,   "ev",           true),
        sug("shuttle_service",     "Shuttle Service",              ProjectAmenityCategory.PARKING_TRANSPORT,   "shuttle",      true),

        // PET_FRIENDLY
        sug("pet_park",            "Pet Park",                     ProjectAmenityCategory.PET_FRIENDLY,        "pet_park",     true),
        sug("pet_grooming",        "Pet Grooming Station",         ProjectAmenityCategory.PET_FRIENDLY,        "pet_groom",    true),

        // COMMERCIAL_RETAIL
        sug("mini_market",         "Mini Market / Convenience Store", ProjectAmenityCategory.COMMERCIAL_RETAIL,"market",       false),
        sug("cafe",                "Café",                         ProjectAmenityCategory.COMMERCIAL_RETAIL,   "cafe",         false),
        sug("pharmacy",            "Pharmacy",                     ProjectAmenityCategory.COMMERCIAL_RETAIL,   "pharmacy",     true)
    );

    private static AmenityCategoryMetaResponse amenityCat(ProjectAmenityCategory value, String label, int displayOrder) {
        return new AmenityCategoryMetaResponse(value, label, displayOrder);
    }

    private static AmenitySuggestionResponse sug(String code, String label, ProjectAmenityCategory category,
                                                  String iconKey, boolean rare) {
        return new AmenitySuggestionResponse(code, label, category, category.toLabel(), iconKey, rare);
    }
}
