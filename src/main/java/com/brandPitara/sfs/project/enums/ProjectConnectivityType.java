package com.brandPitara.sfs.project.enums;

public enum ProjectConnectivityType {

  // Transit
  METRO,
  BUS_STOP,
  RAILWAY_STATION,
  AIRPORT,
  HIGHWAY,

  // Education
  SCHOOL,
  COLLEGE,
  UNIVERSITY,

  // Healthcare
  HOSPITAL,
  CLINIC,
  PHARMACY,

  // Work / commercial
  BUSINESS_HUB,
  TECH_PARK,
  OFFICE_HUB,
  OFFICE_SPACE,

  // Retail / lifestyle
  MALL,
  SUPERMARKET,
  RESTAURANT,
  CAFE,
  MARKET,
  RETAIL_SHOP,
  GROCERY_STORE,
  CONVENIENCE_STORE,

  // Recreation
  PARK,
  GYM,
  FITNESS_CENTER,
  STADIUM,

  // Safety
  POLICE_STATION,
  FIRE_STATION,

  // Finance
  BANK,
  ATM,

  // Places of worship / culture
  TEMPLE,
  CHURCH,
  MOSQUE,

  LANDMARK,
  OTHER;

  public String toLabel() {
    return switch (this) {
      case METRO -> "Metro";
      case BUS_STOP -> "Bus Stop";
      case RAILWAY_STATION -> "Railway Station";
      case AIRPORT -> "Airport";
      case HIGHWAY -> "Highway";
      case SCHOOL -> "School";
      case COLLEGE -> "College";
      case UNIVERSITY -> "University";
      case HOSPITAL -> "Hospital";
      case CLINIC -> "Clinic";
      case PHARMACY -> "Pharmacy";
      case BUSINESS_HUB -> "Business Hub";
      case TECH_PARK -> "Tech Park";
      case OFFICE_HUB -> "Office Hub";
      case OFFICE_SPACE -> "Office Space";
      case MALL -> "Mall";
      case SUPERMARKET -> "Supermarket";
      case RESTAURANT -> "Restaurant";
      case CAFE -> "Cafe";
      case MARKET -> "Market";
      case RETAIL_SHOP -> "Retail Shop";
      case GROCERY_STORE -> "Grocery Store";
      case CONVENIENCE_STORE -> "Convenience Store";
      case PARK -> "Park";
      case GYM -> "Gym";
      case FITNESS_CENTER -> "Fitness Center";
      case STADIUM -> "Stadium";
      case POLICE_STATION -> "Police Station";
      case FIRE_STATION -> "Fire Station";
      case BANK -> "Bank";
      case ATM -> "ATM";
      case TEMPLE -> "Temple";
      case CHURCH -> "Church";
      case MOSQUE -> "Mosque";
      case LANDMARK -> "Landmark";
      case OTHER -> "Other";
    };
  }
}
