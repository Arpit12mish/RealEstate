package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;

public final class ProjectConnectivityCategoryMapper {

  private ProjectConnectivityCategoryMapper() {}

  public static ProjectConnectivityCategory fromType(ProjectConnectivityType type) {
    if (type == null) {
      return ProjectConnectivityCategory.LIFESTYLE;
    }

    return switch (type) {
      case METRO, BUS_STOP, RAILWAY_STATION, AIRPORT, HIGHWAY ->
          ProjectConnectivityCategory.TRANSIT;

      case SCHOOL ->
          ProjectConnectivityCategory.SCHOOLS;

      case COLLEGE, UNIVERSITY ->
          ProjectConnectivityCategory.COLLEGES;

      case HOSPITAL, CLINIC, PHARMACY ->
          ProjectConnectivityCategory.HOSPITALS;

      case PARK ->
          ProjectConnectivityCategory.PARKS;

      case GYM, FITNESS_CENTER ->
          ProjectConnectivityCategory.GYMS;

      case RETAIL_SHOP, MARKET ->
          ProjectConnectivityCategory.RETAIL;

      case SUPERMARKET, GROCERY_STORE, CONVENIENCE_STORE ->
          ProjectConnectivityCategory.DAILY_NEEDS;

      case MALL ->
          ProjectConnectivityCategory.MALLS;

      case RESTAURANT, CAFE ->
          ProjectConnectivityCategory.RESTAURANTS;

      case BUSINESS_HUB, TECH_PARK, OFFICE_HUB, OFFICE_SPACE ->
          ProjectConnectivityCategory.OFFICES;

      case BANK, ATM ->
          ProjectConnectivityCategory.BANKS;

      case POLICE_STATION, FIRE_STATION ->
          ProjectConnectivityCategory.SAFETY;

      default ->
          ProjectConnectivityCategory.LIFESTYLE;
    };
  }
}
