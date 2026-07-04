package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanStatResponse;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import com.brandPitara.sfs.project.enums.ParkingType;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProjectMasterPlanMapper {

  private static final String DEFAULT_TITLE = "Master Plan";
  private static final String DEFAULT_SUBTITLE = "Site layout, towers & open spaces";

  public static ProjectMasterPlanResponse toPublicResponse(ProjectMasterPlanEntity e) {
    if (e == null || !Boolean.TRUE.equals(e.getActive()) || Boolean.TRUE.equals(e.getDeleted())) {
      return null;
    }

    List<ProjectMasterPlanStatResponse> stats = buildStats(e);
    if (!StringUtils.hasText(e.getMasterPlanImageUrl()) && stats.isEmpty()) {
      return null;
    }

    return ProjectMasterPlanResponse.builder()
        .title(withDefault(e.getTitle(), DEFAULT_TITLE))
        .subtitle(withDefault(e.getSubtitle(), DEFAULT_SUBTITLE))
        .description(e.getDescription())
        .imageUrl(e.getMasterPlanImageUrl())
        .imageCaption(e.getImageCaption())
        .imageAltText(e.getImageAltText())
        .expandable(StringUtils.hasText(e.getMasterPlanImageUrl()))
        .verified(Boolean.TRUE.equals(e.getVerified()))
        .sourceLabel(e.getSourceLabel())
        .lastVerifiedAt(e.getLastVerifiedAt())
        .stats(stats)
        .build();
  }

  public static ProjectMasterPlanResponse toDashboardResponse(ProjectMasterPlanEntity e) {
    if (e == null) {
      return null;
    }

    return ProjectMasterPlanResponse.builder()
        .id(e.getId())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .title(withDefault(e.getTitle(), DEFAULT_TITLE))
        .subtitle(withDefault(e.getSubtitle(), DEFAULT_SUBTITLE))
        .description(e.getDescription())
        .imageUrl(e.getMasterPlanImageUrl())
        .imageCaption(e.getImageCaption())
        .imageAltText(e.getImageAltText())
        .expandable(StringUtils.hasText(e.getMasterPlanImageUrl()))
        .verified(Boolean.TRUE.equals(e.getVerified()))
        .sourceLabel(e.getSourceLabel())
        .lastVerifiedAt(e.getLastVerifiedAt())
        .stats(buildStats(e))
        .sourceDocumentUrl(e.getSourceDocumentUrl())
        .remarks(e.getRemarks())
        .active(Boolean.TRUE.equals(e.getActive()))
        .approvalStatus(e.getApprovalStatus())
        .totalUnits(e.getTotalUnits())
        .totalTowers(e.getTotalTowers())
        .totalFloors(e.getTotalFloors())
        .parkAreaValue(e.getParkAreaValue())
        .parkAreaUnit(e.getParkAreaUnit())
        .totalLandAreaValue(e.getTotalLandAreaValue())
        .totalLandAreaUnit(e.getTotalLandAreaUnit())
        .openSpaceAreaValue(e.getOpenSpaceAreaValue())
        .openSpaceAreaUnit(e.getOpenSpaceAreaUnit())
        .greenAreaValue(e.getGreenAreaValue())
        .greenAreaUnit(e.getGreenAreaUnit())
        .clubhouseAreaValue(e.getClubhouseAreaValue())
        .clubhouseAreaUnit(e.getClubhouseAreaUnit())
        .amenityAreaValue(e.getAmenityAreaValue())
        .amenityAreaUnit(e.getAmenityAreaUnit())
        .roadWidthValue(e.getRoadWidthValue())
        .roadWidthUnit(e.getRoadWidthUnit())
        .waterSource(e.getWaterSource())
        .parkingType(e.getParkingType())
        .totalParkingSlots(e.getTotalParkingSlots())
        .visitorParkingSlots(e.getVisitorParkingSlots())
        .basementLevels(e.getBasementLevels())
        .entryExitGates(e.getEntryExitGates())
        .liftCount(e.getLiftCount())
        .phaseCount(e.getPhaseCount())
        .currentPhase(e.getCurrentPhase())
        .openSpacePercent(e.getOpenSpacePercent())
        .greenCoveragePercent(e.getGreenCoveragePercent())
        .vastuCompliant(e.getVastuCompliant())
        .gatedCommunity(e.getGatedCommunity())
        .boundaryWall(e.getBoundaryWall())
        .fireTenderMovement(e.getFireTenderMovement())
        .sewageTreatmentPlant(e.getSewageTreatmentPlant())
        .rainwaterHarvesting(e.getRainwaterHarvesting())
        .powerBackup(e.getPowerBackup())
        .build();
  }

  public static List<ProjectMasterPlanStatResponse> buildStats(ProjectMasterPlanEntity e) {
    List<ProjectMasterPlanStatResponse> stats = new ArrayList<>();

    addInteger(stats, "TOTAL_UNITS", "Total Units", e.getTotalUnits(), 10);
    addArea(stats, "PARK_AREA", "Park Area", e.getParkAreaValue(), e.getParkAreaUnit(), 20);
    addInteger(stats, "TOTAL_TOWERS", "Total Towers", e.getTotalTowers(), 30);
    addInteger(stats, "TOTAL_FLOORS", "Total Floors", e.getTotalFloors(), 40);
    addText(stats, "WATER_SOURCE", "Water Source", e.getWaterSource(), 50);

    addArea(stats, "TOTAL_LAND_AREA", "Total Land Area", e.getTotalLandAreaValue(), e.getTotalLandAreaUnit(), 60);
    addArea(stats, "OPEN_SPACE_AREA", "Open Space Area", e.getOpenSpaceAreaValue(), e.getOpenSpaceAreaUnit(), 70);
    addArea(stats, "GREEN_AREA", "Green Area", e.getGreenAreaValue(), e.getGreenAreaUnit(), 80);
    addArea(stats, "CLUBHOUSE_AREA", "Clubhouse Area", e.getClubhouseAreaValue(), e.getClubhouseAreaUnit(), 90);
    addArea(stats, "AMENITY_AREA", "Amenity Area", e.getAmenityAreaValue(), e.getAmenityAreaUnit(), 100);
    addArea(stats, "ROAD_WIDTH", "Road Width", e.getRoadWidthValue(), e.getRoadWidthUnit(), 110);

    addParking(stats, e.getParkingType(), 120);
    addInteger(stats, "TOTAL_PARKING_SLOTS", "Total Parking Slots", e.getTotalParkingSlots(), 130);
    addInteger(stats, "VISITOR_PARKING_SLOTS", "Visitor Parking Slots", e.getVisitorParkingSlots(), 140);
    addInteger(stats, "BASEMENT_LEVELS", "Basement Levels", e.getBasementLevels(), 150);
    addInteger(stats, "ENTRY_EXIT_GATES", "Entry/Exit Gates", e.getEntryExitGates(), 160);
    addInteger(stats, "LIFT_COUNT", "Lift Count", e.getLiftCount(), 170);
    addInteger(stats, "PHASE_COUNT", "Phase Count", e.getPhaseCount(), 180);
    addText(stats, "CURRENT_PHASE", "Current Phase", e.getCurrentPhase(), 190);
    addPercent(stats, "OPEN_SPACE_PERCENT", "Open Space", e.getOpenSpacePercent(), 200);
    addPercent(stats, "GREEN_COVERAGE_PERCENT", "Green Coverage", e.getGreenCoveragePercent(), 210);

    addBoolean(stats, "VASTU_COMPLIANT", "Vastu Compliant", e.getVastuCompliant(), 220);
    addBoolean(stats, "GATED_COMMUNITY", "Gated Community", e.getGatedCommunity(), 230);
    addBoolean(stats, "BOUNDARY_WALL", "Boundary Wall", e.getBoundaryWall(), 240);
    addBoolean(stats, "FIRE_TENDER_MOVEMENT", "Fire Tender Movement", e.getFireTenderMovement(), 250);
    addBoolean(stats, "SEWAGE_TREATMENT_PLANT", "Sewage Treatment Plant", e.getSewageTreatmentPlant(), 260);
    addBoolean(stats, "RAINWATER_HARVESTING", "Rainwater Harvesting", e.getRainwaterHarvesting(), 270);
    addBoolean(stats, "POWER_BACKUP", "Power Backup", e.getPowerBackup(), 280);

    return stats.stream()
        .sorted(Comparator.comparing(ProjectMasterPlanStatResponse::getDisplayOrder))
        .toList();
  }

  private static void addInteger(List<ProjectMasterPlanStatResponse> stats, String key, String label,
                                 Integer rawValue, int order) {
    if (rawValue == null) return;
    stats.add(stat(key, label, rawValue.toString(), rawValue, null, order));
  }

  private static void addText(List<ProjectMasterPlanStatResponse> stats, String key, String label,
                              String value, int order) {
    if (!StringUtils.hasText(value)) return;
    stats.add(stat(key, label, value.trim(), null, null, order));
  }

  private static void addArea(List<ProjectMasterPlanStatResponse> stats, String key, String label,
                              BigDecimal rawValue, MasterPlanAreaUnit unit, int order) {
    if (rawValue == null) return;
    String formatted = formatDecimal(rawValue);
    String display = unit != null ? formatted + " " + unit.displayLabel() : formatted;
    stats.add(stat(key, label, display, rawValue, unit != null ? unit.name() : null, order));
  }

  private static void addPercent(List<ProjectMasterPlanStatResponse> stats, String key, String label,
                                 BigDecimal rawValue, int order) {
    if (rawValue == null) return;
    stats.add(stat(key, label, formatDecimal(rawValue) + "%", rawValue, "PERCENT", order));
  }

  private static void addParking(List<ProjectMasterPlanStatResponse> stats, ParkingType parkingType, int order) {
    if (parkingType == null) return;
    stats.add(stat("PARKING_TYPE", "Parking Type", parkingType.displayLabel(), parkingType.name(), null, order));
  }

  private static void addBoolean(List<ProjectMasterPlanStatResponse> stats, String key, String label,
                                 Boolean rawValue, int order) {
    if (rawValue == null) return;
    stats.add(stat(key, label, Boolean.TRUE.equals(rawValue) ? "Yes" : "No", rawValue, null, order));
  }

  private static ProjectMasterPlanStatResponse stat(String key, String label, String value, Object rawValue,
                                                    String unit, int order) {
    return ProjectMasterPlanStatResponse.builder()
        .key(key)
        .label(label)
        .value(value)
        .rawValue(rawValue)
        .unit(unit)
        .displayOrder(order)
        .build();
  }

  private static String formatDecimal(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static String withDefault(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }
}
