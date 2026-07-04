package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.home.cards.dto.CardActionDto;
import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.repository.ProjectNearbyListingProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ProjectNearbyListingMapper {

  private ProjectNearbyListingMapper() {
  }

  public static ProjectNearbyListingCardDto toCard(ProjectNearbyListingProjection row) {
    if (row == null) {
      return null;
    }

    Double distanceKm = row.getDistanceKm() == null ? null : round(row.getDistanceKm(), 2);
    String projectPath = "/projects/" + (
        hasText(row.getProjectSlug()) ? row.getProjectSlug().trim() : String.valueOf(row.getProjectId())
    );

    return ProjectNearbyListingCardDto.builder()
        .projectId(row.getProjectId())
        .projectName(row.getProjectName())
        .projectSlug(row.getProjectSlug())
        .coverImageUrl(row.getCoverImageUrl())
        .addressLine(row.getAddressLine())
        .cityName(row.getCityName())
        .localityName(null)
        .builderId(row.getBuilderId())
        .builderName(row.getBuilderName())
        .builderLogoUrl(row.getBuilderLogoUrl())
        .priceMin(row.getPriceMin())
        .priceMax(row.getPriceMax())
        .priceLabel(formatPriceLabel(row.getPriceMin(), row.getPriceMax()))
        .unitSummary(buildUnitSummary(row))
        .latitude(row.getLatitude())
        .longitude(row.getLongitude())
        .distanceKm(distanceKm)
        .distanceLabel(formatDistanceLabel(distanceKm))
        .verified(Boolean.TRUE.equals(row.getVerified()))
        .action(CardActionDto.builder()
            .type("NAVIGATE")
            .target("PROJECT_DETAIL")
            .path(projectPath)
            .build())
        .build();
  }

  private static String formatPriceLabel(Long priceMin, Long priceMax) {
    Long price = priceMin != null ? priceMin : priceMax;
    if (price == null || price <= 0) {
      return null;
    }

    if (price >= 10_000_000L) {
      return "₹" + compact(price / 10_000_000.0) + " Cr";
    }

    if (price >= 100_000L) {
      return "₹" + compact(price / 100_000.0) + " L";
    }

    return "₹" + price;
  }

  private static String compact(double value) {
    BigDecimal rounded = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
    return rounded.toPlainString();
  }

  private static String buildUnitSummary(ProjectNearbyListingProjection row) {
    String unitLabel = normalizeUnitLabel(row);
    String areaLabel = formatArea(firstNonNull(row.getSaleableAreaSqft(), row.getSuperAreaSqft(), row.getCarpetAreaSqft()));

    if (hasText(unitLabel) && hasText(areaLabel)) {
      return unitLabel + " · " + areaLabel;
    }
    return hasText(unitLabel) ? unitLabel : areaLabel;
  }

  private static String normalizeUnitLabel(ProjectNearbyListingProjection row) {
    if (row.getBedrooms() != null && row.getBedrooms() > 0) {
      return row.getBedrooms() + " BHK";
    }

    if (hasText(row.getUnitLabel())) {
      return row.getUnitLabel().trim();
    }

    if (!hasText(row.getUnitConfigurationType())) {
      return null;
    }

    return row.getUnitConfigurationType()
        .replace("_BHK", " BHK")
        .replace("_", " ");
  }

  private static String formatArea(BigDecimal area) {
    if (area == null || area.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return area.setScale(0, RoundingMode.HALF_UP).toPlainString() + " sqft";
  }

  @SafeVarargs
  private static <T> T firstNonNull(T... values) {
    if (values == null) {
      return null;
    }
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static String formatDistanceLabel(Double distanceKm) {
    if (distanceKm == null) {
      return null;
    }
    if (distanceKm < 1.0) {
      return Math.max(1, (int) Math.round(distanceKm * 1000.0)) + " m";
    }
    return compact(distanceKm) + " km";
  }

  private static Double round(Double value, int scale) {
    return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
