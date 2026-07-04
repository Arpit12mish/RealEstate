package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.project.dto.*;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.project.mapper.ProjectMapper;
import com.brandPitara.sfs.project.mapper.ProjectMediaMapper;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import com.brandPitara.sfs.project.service.ProjectDetailComposer;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import com.brandPitara.sfs.project.service.ProjectMasterPlanService;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import org.springframework.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectDetailComposerImpl implements ProjectDetailComposer {

  private static final int DEFAULT_DOWN_PAYMENT_PERCENT = 20;
  private static final double DEFAULT_ANNUAL_INTEREST_RATE = 8.5;
  private static final int DEFAULT_TENURE_YEARS = 20;

  private final ProjectFloorPlanService projectFloorPlanService;
  private final ProjectConnectivityService projectConnectivityService;
  private final ProjectMeterService projectMeterService;
  private final ProjectMasterPlanService projectMasterPlanService;

  @Override
  public ProjectResponse compose(ProjectEntity project, List<ProjectMediaEntity> media) {
    ProjectResponse response = ProjectMapper.toResponse(project, media);

    ProjectMeterDetailResponse meterDetail = safeGetMeterDetail(project.getId());
    ProjectConnectivityResponse connectivity = safeGetConnectivity(project.getId());

    response.setPricing(buildPricing(project, meterDetail));
    response.setLocation(buildLocation(project, connectivity));
    response.setFloorPlanGroups(buildFloorPlanGroups(project.getId()));
    response.setConnectivity(connectivity);
    response.setGlimpses(buildGlimpses(media));
    response.setAmenities(meterDetail != null ? meterDetail.getAmenities() : null);
    response.setMasterPlan(safeGetDashboardMasterPlan(project.getId()));

    return response;
  }

  @Override
  public ProjectPublicResponse composePublic(ProjectEntity project, List<ProjectMediaEntity> media) {
    ProjectPublicResponse response = ProjectMapper.toPublicResponse(project, media);

    ProjectMeterDetailResponse meterDetail = safeGetMeterDetail(project.getId());
    ProjectConnectivityResponse connectivity = safeGetConnectivity(project.getId());

    response.setPricing(buildPricing(project, meterDetail));
    response.setLocation(buildLocation(project, connectivity));
    response.setFloorPlanGroups(buildFloorPlanGroups(project.getId()));
    response.setConnectivity(connectivity);
    response.setGlimpses(buildGlimpses(media));
    response.setAmenities(meterDetail != null ? meterDetail.getAmenities() : null);
    response.setMasterPlan(safeGetMasterPlan(project.getId()));

    return response;
  }

  @Override
  public ProjectResponse composeForPreview(ProjectEntity project, List<ProjectMediaEntity> media,
                                           @Nullable ProjectMeterDetailResponse meterDetail) {
    ProjectResponse response = ProjectMapper.toResponse(project, media);
    ProjectConnectivityResponse connectivity = safeGetDashboardConnectivity(project.getId());

    response.setPricing(buildPricing(project, meterDetail));
    response.setLocation(buildLocation(project, connectivity));
    response.setFloorPlanGroups(buildDashboardPreviewFloorPlanGroups(project.getId()));
    response.setConnectivity(connectivity);
    response.setGlimpses(buildGlimpses(media));
    response.setAmenities(meterDetail != null ? meterDetail.getAmenities() : null);
    response.setMasterPlan(safeGetDashboardPreviewMasterPlan(project.getId()));

    return response;
  }

  private ProjectConnectivityResponse safeGetConnectivity(Long projectId) {
    try {
      return projectConnectivityService.publicGet(projectId);
    } catch (Exception ignored) {
      return null;
    }
  }

  private ProjectConnectivityResponse safeGetDashboardConnectivity(Long projectId) {
    try {
      return projectConnectivityService.adminGet(projectId);
    } catch (Exception ignored) {
      return null;
    }
  }

  private ProjectMasterPlanResponse safeGetMasterPlan(Long projectId) {
    try {
      return projectMasterPlanService.publicGet(projectId);
    } catch (Exception ignored) {
      return null;
    }
  }

  private ProjectMasterPlanResponse safeGetDashboardMasterPlan(Long projectId) {
    try {
      return projectMasterPlanService.adminGet(projectId);
    } catch (Exception ignored) {
      return null;
    }
  }

  private ProjectMasterPlanResponse safeGetDashboardPreviewMasterPlan(Long projectId) {
    try {
      return projectMasterPlanService.dashboardPreviewGet(projectId);
    } catch (Exception ignored) {
      return null;
    }
  }

  private ProjectPricingSummaryResponse buildPricing(ProjectEntity project, ProjectMeterDetailResponse meterDetail) {
    Long minPrice = project.getPriceMin();
    Long maxPrice = project.getPriceMax();

    Long averageAreaPrice = project.getAveragePricePerSqft();
    Double appreciationPercent = null;

    if (meterDetail != null && meterDetail.getPriceInsights() != null) {
      if (averageAreaPrice == null) {
        averageAreaPrice = meterDetail.getPriceInsights().getAverageAreaPrice();
      }
      appreciationPercent = meterDetail.getPriceInsights().getAppreciationPercent();
    }

    // Use dashboard-stored EMI values when available; fall back to formula-derived values.
    Long emiMin = project.getMonthlyEmiMin() != null ? project.getMonthlyEmiMin() : calculateEmi(minPrice);
    Long emiMax = project.getMonthlyEmiMax() != null ? project.getMonthlyEmiMax() : calculateEmi(maxPrice);

    return ProjectPricingSummaryResponse.builder()
        .minPrice(minPrice)
        .maxPrice(maxPrice)
        .averageAreaPrice(averageAreaPrice)
        .estimatedMonthlyEmiMin(emiMin)
        .estimatedMonthlyEmiMax(emiMax)
        .appreciationPercent(appreciationPercent)
        .downPaymentPercent(DEFAULT_DOWN_PAYMENT_PERCENT)
        .annualInterestRate(DEFAULT_ANNUAL_INTEREST_RATE)
        .tenureYears(DEFAULT_TENURE_YEARS)
        .build();
  }

  private ProjectLocationResponse buildLocation(ProjectEntity project, ProjectConnectivityResponse connectivity) {
    return ProjectLocationResponse.builder()
        .addressLine(project.getAddressLine())
        .cityId(project.getCity() != null ? project.getCity().getId() : null)
        .cityName(project.getCity() != null ? project.getCity().getName() : null)
        .latitude(project.getLatitude())
        .longitude(project.getLongitude())
        .mapImageUrl(connectivity != null ? connectivity.getMapImageUrl() : null)
        .build();
  }

  private List<ProjectFloorPlanGroupResponse> buildFloorPlanGroups(Long projectId) {
    List<ProjectFloorPlanResponse> floorPlans = projectFloorPlanService.publicList(projectId);
    return groupFloorPlans(floorPlans);
  }

  private List<ProjectFloorPlanGroupResponse> buildDashboardPreviewFloorPlanGroups(Long projectId) {
    List<ProjectFloorPlanResponse> floorPlans = projectFloorPlanService.dashboardPreviewList(projectId);
    return groupFloorPlans(floorPlans);
  }

  private List<ProjectFloorPlanGroupResponse> groupFloorPlans(List<ProjectFloorPlanResponse> floorPlans) {
    if (floorPlans == null || floorPlans.isEmpty()) {
      return List.of();
    }

    Map<String, List<ProjectFloorPlanResponse>> grouped = floorPlans.stream()
        .collect(Collectors.groupingBy(
            fp -> fp.getUnitConfigurationType() != null
                ? fp.getUnitConfigurationType().name()
                : normalizeGroupKey(fp.getUnitLabel(), fp.getTitle(), fp.getFloorCode()),
            LinkedHashMap::new,
            Collectors.toList()
        ));

    return grouped.entrySet().stream()
        .map(entry -> {
          String key = entry.getKey();
          List<ProjectFloorPlanResponse> items = entry.getValue();
          String label = items.stream()
              .filter(fp -> fp.getUnitConfigurationType() != null)
              .map(fp -> fp.getUnitConfigurationTypeLabel())
              .filter(l -> l != null)
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

  private List<ProjectMediaResponse> buildGlimpses(List<ProjectMediaEntity> media) {
    if (media == null || media.isEmpty()) {
      return List.of();
    }

    return media.stream()
        .filter(Objects::nonNull)
        .filter(m -> Boolean.TRUE.equals(m.getActive()))
        .filter(m -> Boolean.FALSE.equals(m.getDeleted()))
        .filter(m -> m.getMediaType() == ProjectMediaType.IMAGE)
        .sorted(Comparator
            .comparing(ProjectMediaEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(ProjectMediaEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(ProjectMediaMapper::toResponse)
        .toList();
  }

  private ProjectMeterDetailResponse safeGetMeterDetail(Long projectId) {
    try {
      return projectMeterService.publicGetMeterDetail(projectId);
    } catch (Exception ignored) {
      return null;
    }
  }

  private Long calculateEmi(Long propertyPrice) {
    if (propertyPrice == null || propertyPrice <= 0) {
      return null;
    }

    double principal = propertyPrice * ((100.0 - DEFAULT_DOWN_PAYMENT_PERCENT) / 100.0);
    double monthlyRate = DEFAULT_ANNUAL_INTEREST_RATE / 12.0 / 100.0;
    int months = DEFAULT_TENURE_YEARS * 12;

    if (monthlyRate <= 0 || months <= 0) {
      return Math.round(principal / Math.max(months, 1));
    }

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

    if (normalized.contains("STUDIO")) return "STUDIO";
    if (normalized.contains("OFFICE")) return "OFFICE";
    if (normalized.contains("1 BHK") || normalized.contains("1BHK")) return "1_BHK";
    if (normalized.contains("2 BHK") || normalized.contains("2BHK")) return "2_BHK";
    if (normalized.contains("3 BHK") || normalized.contains("3BHK")) return "3_BHK";
    if (normalized.contains("4 BHK") || normalized.contains("4BHK")) return "4_BHK";
    if (normalized.contains("PENTHOUSE")) return "PENTHOUSE";
    if (normalized.contains("VILLA")) return "VILLA";

    return normalized.replaceAll("\\s+", "_");
  }

  private String toReadableGroupLabel(String key) {
    if (key == null || key.isBlank()) return "Other";
    return key.replace("_", " ");
  }

  private String firstNonBlank(String... values) {
    if (values == null) return null;
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
