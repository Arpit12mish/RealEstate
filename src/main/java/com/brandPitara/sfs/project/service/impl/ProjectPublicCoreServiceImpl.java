package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanGroupResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectLocationResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectPricingSummaryResponse;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.service.ProjectPublicCoreService;
import com.brandPitara.sfs.project.service.model.ProjectDetailAnalyticsData;
import com.brandPitara.sfs.project.service.model.ProjectPublicContentData;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import com.brandPitara.sfs.project.service.model.ProjectPublicSectionFailure;
import com.brandPitara.sfs.project.service.reader.ProjectDetailAnalyticsReader;
import com.brandPitara.sfs.project.service.reader.ProjectPublicBaseReader;
import com.brandPitara.sfs.project.service.reader.ProjectPublicContentReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectPublicCoreServiceImpl implements ProjectPublicCoreService {

    private static final int DEFAULT_DOWN_PAYMENT_PERCENT = 20;
    private static final double DEFAULT_ANNUAL_INTEREST_RATE = 8.5;
    private static final int DEFAULT_TENURE_YEARS = 20;
    private static final Pattern BHK_PATTERN = Pattern.compile("\\b(5\\+|[1-5](?:\\.5)?)\\s*BHK\\b");

    private final ProjectPublicBaseReader baseReader;
    private final ProjectDetailAnalyticsReader analyticsReader;
    private final ProjectPublicContentReader contentReader;

    @Override
    public ProjectPublicCoreData getById(Long projectId) {
        return assemble(baseReader.readById(projectId));
    }

    @Override
    public ProjectPublicCoreData getBySlug(String projectSlug) {
        return assemble(baseReader.readBySlug(projectSlug));
    }

    private ProjectPublicCoreData assemble(ProjectPublicCoreData base) {
        Long projectId = base.getId();
        EnumSet<ProjectPublicSectionFailure> failures = EnumSet.noneOf(ProjectPublicSectionFailure.class);

        ProjectDetailAnalyticsData analytics = null;
        try {
            analytics = analyticsReader.read(projectId);
        } catch (Exception ignored) {
            failures.add(ProjectPublicSectionFailure.ANALYTICS);
        }

        ProjectPublicContentData content = contentReader.readAfterVisibilityCheck(base);
        failures.addAll(content.failedSections());

        return base.toBuilder()
                .pricing(buildPricing(base, analytics))
                .location(buildLocation(base, content.connectivity()))
                .floorPlanGroups(groupFloorPlans(content.floorPlans()))
                .connectivity(content.connectivity())
                .amenities(analytics != null ? analytics.amenities() : null)
                .masterPlan(content.masterPlan())
                .failedSections(failures.isEmpty() ? Set.of() : Set.copyOf(failures))
                .build();
    }

    private ProjectPricingSummaryResponse buildPricing(
            ProjectPublicCoreData project,
            ProjectDetailAnalyticsData analytics
    ) {
        Long averageAreaPrice = project.getAveragePricePerSqft();
        if (averageAreaPrice == null && analytics != null) {
            averageAreaPrice = analytics.snapshotAverageAreaPrice();
        }

        Long emiMin = project.getMonthlyEmiMin() != null
                ? project.getMonthlyEmiMin()
                : calculateEmi(project.getPriceMin());
        Long emiMax = project.getMonthlyEmiMax() != null
                ? project.getMonthlyEmiMax()
                : calculateEmi(project.getPriceMax());

        return ProjectPricingSummaryResponse.builder()
                .minPrice(project.getPriceMin())
                .maxPrice(project.getPriceMax())
                .averageAreaPrice(averageAreaPrice)
                .estimatedMonthlyEmiMin(emiMin)
                .estimatedMonthlyEmiMax(emiMax)
                .appreciationPercent(analytics != null
                        ? analytics.snapshotAppreciationPercent()
                        : null)
                .downPaymentPercent(DEFAULT_DOWN_PAYMENT_PERCENT)
                .annualInterestRate(DEFAULT_ANNUAL_INTEREST_RATE)
                .tenureYears(DEFAULT_TENURE_YEARS)
                .build();
    }

    private ProjectLocationResponse buildLocation(
            ProjectPublicCoreData project,
            ProjectConnectivityResponse connectivity
    ) {
        return ProjectLocationResponse.builder()
                .addressLine(project.getAddressLine())
                .cityId(project.getCityId())
                .cityName(project.getCityName())
                .latitude(project.getLatitude())
                .longitude(project.getLongitude())
                .mapImageUrl(connectivity != null ? connectivity.getMapImageUrl() : null)
                .build();
    }

    private List<ProjectFloorPlanGroupResponse> groupFloorPlans(List<ProjectFloorPlanResponse> floorPlans) {
        if (floorPlans == null || floorPlans.isEmpty()) {
            return List.of();
        }

        Map<String, List<ProjectFloorPlanResponse>> grouped = floorPlans.stream()
                .collect(Collectors.groupingBy(
                        floorPlan -> floorPlan.getUnitConfigurationType() != null
                                ? floorPlan.getUnitConfigurationType().name()
                                : normalizeGroupKey(
                                        floorPlan.getUnitLabel(),
                                        floorPlan.getTitle(),
                                        floorPlan.getFloorCode()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    List<ProjectFloorPlanResponse> items = List.copyOf(entry.getValue());
                    String label = items.stream()
                            .filter(item -> item.getUnitConfigurationType() != null)
                            .map(ProjectFloorPlanResponse::getUnitConfigurationTypeLabel)
                            .filter(candidate -> candidate != null)
                            .findFirst()
                            .orElse(toReadableGroupLabel(key));
                    return ProjectFloorPlanGroupResponse.builder()
                            .groupKey(key)
                            .groupLabel(label)
                            .items(items)
                            .build();
                })
                .toList();
    }

    private Long calculateEmi(Long propertyPrice) {
        if (propertyPrice == null || propertyPrice <= 0) {
            return null;
        }

        double principal = propertyPrice * ((100.0 - DEFAULT_DOWN_PAYMENT_PERCENT) / 100.0);
        double monthlyRate = DEFAULT_ANNUAL_INTEREST_RATE / 12.0 / 100.0;
        int months = DEFAULT_TENURE_YEARS * 12;
        double emi = principal * monthlyRate * Math.pow(1 + monthlyRate, months)
                / (Math.pow(1 + monthlyRate, months) - 1);
        return Math.round(emi);
    }

    private String normalizeGroupKey(String unitLabel, String title, String floorCode) {
        String raw = firstNonBlank(unitLabel, title, floorCode);
        if (raw == null) {
            return "OTHER";
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        String normalizedForBhk = normalized.replace('_', '.');
        if (normalized.contains("STUDIO")) return "STUDIO";
        if (normalized.contains("OFFICE")) return "OFFICE";
        if (normalized.contains("RETAIL")) return "RETAIL";
        if (normalized.contains("PLOT")) return "PLOT";

        Matcher bhkMatcher = BHK_PATTERN.matcher(normalizedForBhk);
        if (bhkMatcher.find()) {
            return toUnitConfigurationKey(bhkMatcher.group(1));
        }
        if (normalized.contains("PENTHOUSE")) return "PENTHOUSE";
        if (normalized.contains("VILLA")) return "VILLA";
        return normalized.replaceAll("\\s+", "_");
    }

    private String toUnitConfigurationKey(String bhkValue) {
        return switch (bhkValue) {
            case "1" -> "BHK_1";
            case "1.5" -> "BHK_1_5";
            case "2" -> "BHK_2";
            case "2.5" -> "BHK_2_5";
            case "3" -> "BHK_3";
            case "3.5" -> "BHK_3_5";
            case "4" -> "BHK_4";
            case "4.5" -> "BHK_4_5";
            case "5" -> "BHK_5";
            case "5+" -> "BHK_5_PLUS";
            default -> "OTHER";
        };
    }

    private String toReadableGroupLabel(String key) {
        if (key == null || key.isBlank()) {
            return "Other";
        }
        try {
            return UnitConfigurationType.valueOf(key).toLabel();
        } catch (IllegalArgumentException ignored) {
            return key.replace("_", " ");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
