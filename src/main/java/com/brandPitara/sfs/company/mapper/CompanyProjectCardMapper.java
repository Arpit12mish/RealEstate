package com.brandPitara.sfs.company.mapper;

import com.brandPitara.sfs.company.dto.CompanyProjectBudgetDto;
import com.brandPitara.sfs.company.dto.CompanyProjectCardDto;
import com.brandPitara.sfs.company.dto.CompanyProjectStatDto;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

public final class CompanyProjectCardMapper {

  private static final BigDecimal ONE_CRORE = new BigDecimal("10000000");
  private static final BigDecimal ONE_LAKH = new BigDecimal("100000");
  private static final int SHORT_DESCRIPTION_LIMIT = 300;

  private CompanyProjectCardMapper() {}

  public static CompanyProjectCardDto toCard(CompanyProjectEntity project) {
    CompanyEntity company = project.getCompany();
    List<CompanyProjectStatDto> stats = project.getStats() != null ? project.getStats() : List.of();
    CompanyProjectBudgetDto budget = project.getBudget();

    return CompanyProjectCardDto.builder()
        .id(project.getId())
        .name(project.getName())
        .companyId(company != null ? company.getId() : null)
        .companyName(company != null ? company.getName() : null)
        .companyLogoUrl(company != null ? company.getLogoUrl() : null)
        .cityId(project.getCity() != null ? project.getCity().getId() : null)
        .cityName(project.getCity() != null ? project.getCity().getName() : null)
        .addressLine(project.getAddressLine())
        .locationLabel(resolveLocationLabel(project))
        .projectCityLatitude(project.getCity() != null ? project.getCity().getLatitude() : null)
        .projectCityLongitude(project.getCity() != null ? project.getCity().getLongitude() : null)
        .clientName(project.getClientName())
        .projectArea(project.getProjectArea())
        .detail3(project.getDetail3())
        .tags(CompanyProjectTagMapper.toTags(project.getTags()))
        .shortDescription(resolveShortDescription(project))
        .description(clean(project.getDescription()))
        .projectTypeLabel(resolveProjectTypeLabel(project, stats))
        .areaLabel(resolveAreaLabel(project, stats, budget))
        .budgetLabel(resolveBudgetLabel(budget))
        .stats(stats)
        .budget(budget)
        .coverMediaUrl(project.getCoverMediaUrl())
        .coverMediaType(project.getCoverMediaType())
        .build();
  }

  static String resolveProjectTypeLabel(CompanyProjectEntity project, List<CompanyProjectStatDto> stats) {
    String value = statValue(stats, "property type");
    if (value == null) value = statValue(stats, "type");
    if (value != null) return value;
    value = clean(project.getDetail3());
    if (value != null) return value;
    return CompanyProjectTagMapper.toTags(project.getTags()).stream().findFirst().orElse(null);
  }

  static String resolveLocationLabel(CompanyProjectEntity project) {
    String value = clean(project.getLocationLabel());
    if (value != null) return value;
    value = clean(project.getAddressLine());
    if (value != null) return value;
    return project.getCity() != null ? clean(project.getCity().getName()) : null;
  }

  static String resolveAreaLabel(
      CompanyProjectEntity project,
      List<CompanyProjectStatDto> stats,
      CompanyProjectBudgetDto budget
  ) {
    String value = statValue(stats, "area");
    if (value == null) value = clean(project.getProjectArea());
    if (value == null && budget != null) value = clean(budget.getBuiltUpAreaLabel());
    return formatArea(value);
  }

  static String resolveBudgetLabel(CompanyProjectBudgetDto budget) {
    if (budget == null) return null;
    String label = clean(budget.getBudgetLabel());
    if (isMoneyAmount(label)) return label;
    return compactInr(budget.getTotalBudget());
  }

  private static String resolveShortDescription(CompanyProjectEntity project) {
    String value = clean(project.getShortDescription());
    if (value != null) return value;
    value = clean(project.getDescription());
    if (value == null || value.length() <= SHORT_DESCRIPTION_LIMIT) return value;
    return value.substring(0, SHORT_DESCRIPTION_LIMIT - 1).stripTrailing() + "…";
  }

  private static String statValue(List<CompanyProjectStatDto> stats, String labelPart) {
    return stats.stream()
        .filter(stat -> clean(stat.getLabel()) != null && clean(stat.getValue()) != null)
        .filter(stat -> stat.getLabel().trim().toLowerCase(Locale.ROOT).contains(labelPart))
        .map(CompanyProjectStatDto::getValue)
        .map(CompanyProjectCardMapper::clean)
        .findFirst()
        .orElse(null);
  }

  private static String formatArea(String value) {
    value = clean(value);
    if (value == null) return null;
    String numeric = value.replace(",", "");
    if (!numeric.matches("\\d+(?:\\.\\d+)?")) return value;
    try {
      return formatIndianNumber(new BigDecimal(numeric)) + " sqft";
    } catch (NumberFormatException ignored) {
      return value;
    }
  }

  private static boolean isMoneyAmount(String value) {
    if (value == null) return false;
    return value.matches("(?i).*(?:₹|\\bINR\\b|\\bRs\\.?\\s*\\d|\\d\\s*(?:Cr|Crore|Lakh|Lac)\\b).*");
  }

  private static String compactInr(BigDecimal amount) {
    if (amount == null) return null;
    BigDecimal absolute = amount.abs();
    if (absolute.compareTo(ONE_CRORE) >= 0) {
      return "₹" + compact(amount.divide(ONE_CRORE, 2, RoundingMode.HALF_UP)) + "Cr";
    }
    if (absolute.compareTo(ONE_LAKH) >= 0) {
      return "₹" + compact(amount.divide(ONE_LAKH, 2, RoundingMode.HALF_UP)) + "L";
    }
    return "₹" + formatIndianNumber(amount.setScale(0, RoundingMode.HALF_UP));
  }

  private static String compact(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static String formatIndianNumber(BigDecimal value) {
    String plain = value.stripTrailingZeros().toPlainString();
    boolean negative = plain.startsWith("-");
    if (negative) plain = plain.substring(1);
    String[] parts = plain.split("\\.", 2);
    String integer = parts[0];
    if (integer.length() > 3) {
      String lastThree = integer.substring(integer.length() - 3);
      String prefix = integer.substring(0, integer.length() - 3);
      StringBuilder grouped = new StringBuilder();
      int firstGroupLength = prefix.length() % 2;
      int index = 0;
      if (firstGroupLength > 0) {
        grouped.append(prefix, 0, firstGroupLength);
        index = firstGroupLength;
      }
      while (index < prefix.length()) {
        if (!grouped.isEmpty()) grouped.append(',');
        grouped.append(prefix, index, index + 2);
        index += 2;
      }
      integer = grouped + "," + lastThree;
    }
    String decimal = parts.length == 2 ? "." + parts[1] : "";
    return (negative ? "-" : "") + integer + decimal;
  }

  private static String clean(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }
}
