package com.brandPitara.sfs.project.enums;

public enum ProjectConnectivityCategory {
  TRANSIT,
  SCHOOLS,
  COLLEGES,
  HOSPITALS,
  PARKS,
  RETAIL,
  MALLS,
  GYMS,
  OFFICES,
  RESTAURANTS,
  BANKS,
  DAILY_NEEDS,
  LIFESTYLE,
  SAFETY,
  SEARCH;

  public String toLabel() {
    return switch (this) {
      case TRANSIT -> "Transit";
      case SCHOOLS -> "Schools";
      case COLLEGES -> "Colleges";
      case HOSPITALS -> "Hospitals";
      case PARKS -> "Parks";
      case RETAIL -> "Retail Shops";
      case MALLS -> "Malls";
      case GYMS -> "Gyms";
      case OFFICES -> "Offices & IT Parks";
      case RESTAURANTS -> "Restaurants & Cafes";
      case BANKS -> "Banks & ATMs";
      case DAILY_NEEDS -> "Daily Needs";
      case LIFESTYLE -> "Lifestyle";
      case SAFETY -> "Safety";
      case SEARCH -> "Search";
    };
  }

  public String iconKey() {
    return switch (this) {
      case TRANSIT -> "train";
      case SCHOOLS -> "school";
      case COLLEGES -> "graduation-cap";
      case HOSPITALS -> "hospital";
      case PARKS -> "trees";
      case RETAIL -> "store";
      case MALLS -> "shopping-bag";
      case GYMS -> "dumbbell";
      case OFFICES -> "building";
      case RESTAURANTS -> "utensils";
      case BANKS -> "banknote";
      case DAILY_NEEDS -> "shopping-basket";
      case LIFESTYLE -> "map-pin";
      case SAFETY -> "shield";
      case SEARCH -> "search";
    };
  }
}
