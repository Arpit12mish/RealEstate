package com.brandPitara.sfs.projectcompare.builder;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonCellValue;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonProjectHeader;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonRow;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonSection;
import com.brandPitara.sfs.projectcompare.enums.ComparisonSectionKey;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectLocationScoreEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.brandPitara.sfs.projectmeter.enums.ProjectStageStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProjectComparisonSectionBuilder {

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final long CRORE = 10_000_000L;
    private static final long LAKH = 100_000L;

    // ─── Amenity normalization ────────────────────────────────────────────────

    private static final Map<String, String> AMENITY_ALIASES = Map.of(
            "gym",                "gymnasium",
            "club_house",         "clubhouse",
            "pool",               "swimming_pool",
            "kids_play",          "kids_play_area",
            "24x7_security",      "security_24x7",
            "cctv_cameras",       "cctv",
            "ev_charging_station","ev_charging"
    );

    // Higher value = better (used when the same canonical code has multiple DB records for one project)
    private static final Map<ProjectAmenityStatus, Integer> STATUS_PRIORITY = Map.of(
            ProjectAmenityStatus.COMPLETED,     5,
            ProjectAmenityStatus.IN_PROGRESS,   4,
            ProjectAmenityStatus.PLANNED,       3,
            ProjectAmenityStatus.NOT_STARTED,   2,
            ProjectAmenityStatus.NOT_AVAILABLE, 1
    );

    // Amenity codes that appear at the top of the comparison table (in this order)
    private static final List<String> PRIORITY_AMENITY_CODES = List.of(
            "clubhouse", "swimming_pool", "gymnasium", "kids_play_area",
            "landscape_garden", "jogging_track", "sports_zone", "ev_charging",
            "security_24x7", "cctv", "power_backup_24x7", "water_supply_24x7",
            "car_parking", "fire_fighting_systems", "lifts"
    );

    // ─── Public entry points ──────────────────────────────────────────────────

    public List<ComparisonProjectHeader> buildHeaders(
            List<ProjectEntity> projects,
            Map<Long, ProjectMediaEntity> heroImageMap
    ) {
        return projects.stream().map(p -> {
            ProjectMediaEntity img = heroImageMap.get(p.getId());
            return ComparisonProjectHeader.builder()
                    .projectId(p.getId())
                    .name(p.getName())
                    .slug(p.getSlug())
                    .heroImageUrl(img != null ? img.getUrl() : null)
                    .builderName(p.getBuilder() != null ? p.getBuilder().getName() : null)
                    .builderLogoUrl(p.getBuilder() != null ? p.getBuilder().getLogoUrl() : null)
                    .cityName(p.getCity() != null ? p.getCity().getName() : null)
                    .build();
        }).toList();
    }

    public ComparisonSection buildOverview(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        // Core rows — always included regardless of data availability
        rows.add(textRow("name",           "Project Name",   "TEXT", false, projects, p -> cv(p.getName())));
        rows.add(textRow("builder",        "Developer",      "TEXT", false, projects, p -> cv(p.getBuilder() != null ? p.getBuilder().getName() : null)));
        rows.add(textRow("city",           "City",           "TEXT", false, projects, p -> cv(p.getCity() != null ? p.getCity().getName() : null)));
        rows.add(textRow("projectStatus",  "Status",         "BADGE", false, projects, p -> cv(p.getStatus() != null ? formatStatus(p.getStatus().name()) : null)));
        rows.add(textRow("propertyTypes",  "Property Types", "TEXT", false, projects, p -> cv(formatPropertyTypes(p.getPropertyTypes()))));
        rows.add(textRow("reraNumber",     "RERA Number",    "TEXT", false, projects, p -> cv(p.getReraNumber())));
        rows.add(textRow("possessionDate", "Possession Date","DATE", false, projects, p -> cv(formatDate(p.getPossessionDate()))));

        // Optional rows
        addOptionalTextRow(rows, "address", "Address", "TEXT", false, projects, p -> cv(p.getAddressLine()));
        rows.add(buildScoreRow("meterScore", "Meter Score", projects, snapshots));

        return section(ComparisonSectionKey.OVERVIEW, rows);
    }

    public ComparisonSection buildPrice(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        rows.add(textRow("priceRange", "Price Range", "CURRENCY", false, projects, p -> {
            if (p.getPriceMin() == null && p.getPriceMax() == null) return ComparisonCellValue.missing();
            Map<String, Long> priceRaw = new LinkedHashMap<>();
            if (p.getPriceMin() != null) priceRaw.put("min", p.getPriceMin());
            if (p.getPriceMax() != null) priceRaw.put("max", p.getPriceMax());
            return ComparisonCellValue.of(formatPriceRange(p.getPriceMin(), p.getPriceMax()), priceRaw);
        }));

        addOptionalTextRow(rows, "pricePerSqft", "Avg Price / sq.ft", "CURRENCY", false, projects, p ->
                p.getAveragePricePerSqft() != null
                        ? ComparisonCellValue.of("₹" + formatNumber(p.getAveragePricePerSqft()), p.getAveragePricePerSqft())
                        : ComparisonCellValue.missing());

        addOptionalTextRow(rows, "monthlyEmi", "Monthly EMI (est.)", "CURRENCY", false, projects, p -> {
            if (p.getMonthlyEmiMin() == null && p.getMonthlyEmiMax() == null) return ComparisonCellValue.missing();
            Map<String, Long> emiRaw = new LinkedHashMap<>();
            if (p.getMonthlyEmiMin() != null) emiRaw.put("min", p.getMonthlyEmiMin());
            if (p.getMonthlyEmiMax() != null) emiRaw.put("max", p.getMonthlyEmiMax());
            return ComparisonCellValue.of(formatPriceRange(p.getMonthlyEmiMin(), p.getMonthlyEmiMax()), emiRaw);
        });

        addOptionalTextRow(rows, "launchPrice", "Launch Price", "CURRENCY", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            return s != null && s.getLaunchPrice() != null
                    ? ComparisonCellValue.of(formatPrice(s.getLaunchPrice()), s.getLaunchPrice())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "currentPrice", "Current Price", "CURRENCY", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            return s != null && s.getCurrentPrice() != null
                    ? ComparisonCellValue.of(formatPrice(s.getCurrentPrice()), s.getCurrentPrice())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "appreciationPercent3Y", "3Y Appreciation", "PERCENT", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            return s != null && s.getPriceAppreciationPercent() != null
                    ? ComparisonCellValue.of(formatPercent(s.getPriceAppreciationPercent()), s.getPriceAppreciationPercent())
                    : ComparisonCellValue.missing();
        });

        return section(ComparisonSectionKey.PRICE, rows);
    }

    public ComparisonSection buildUnits(
            List<ProjectEntity> projects,
            Map<Long, List<ProjectFloorPlanEntity>> floorPlansMap
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        addOptionalTextRow(rows, "availableConfigurations", "Configurations", "TEXT", false, projects, p -> {
            List<ProjectFloorPlanEntity> fps = floorPlansMap.getOrDefault(p.getId(), List.of());
            String configs = fps.stream()
                    .filter(fp -> fp.getUnitConfigurationType() != null)
                    .map(fp -> fp.getUnitConfigurationType().name())
                    .distinct().sorted()
                    .collect(Collectors.joining(", "));
            return configs.isBlank() ? ComparisonCellValue.missing() : ComparisonCellValue.of(configs, configs);
        });

        addOptionalTextRow(rows, "floorPlanCount", "Floor Plan Count", "TEXT", false, projects, p -> {
            int count = floorPlansMap.getOrDefault(p.getId(), List.of()).size();
            return count > 0
                    ? ComparisonCellValue.of(count + " plans", count)
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "minCarpetArea", "Min Carpet Area", "TEXT", false, projects, p -> {
            Optional<BigDecimal> min = floorPlansMap.getOrDefault(p.getId(), List.of()).stream()
                    .map(ProjectFloorPlanEntity::getCarpetAreaSqft).filter(Objects::nonNull).min(Comparator.naturalOrder());
            return min.map(v -> ComparisonCellValue.of(v + " sq.ft", v)).orElse(ComparisonCellValue.missing());
        });

        addOptionalTextRow(rows, "maxCarpetArea", "Max Carpet Area", "TEXT", false, projects, p -> {
            Optional<BigDecimal> max = floorPlansMap.getOrDefault(p.getId(), List.of()).stream()
                    .map(ProjectFloorPlanEntity::getCarpetAreaSqft).filter(Objects::nonNull).max(Comparator.naturalOrder());
            return max.map(v -> ComparisonCellValue.of(v + " sq.ft", v)).orElse(ComparisonCellValue.missing());
        });

        addOptionalTextRow(rows, "minSuperArea", "Min Super Area", "TEXT", false, projects, p -> {
            Optional<BigDecimal> min = floorPlansMap.getOrDefault(p.getId(), List.of()).stream()
                    .map(ProjectFloorPlanEntity::getSuperAreaSqft).filter(Objects::nonNull).min(Comparator.naturalOrder());
            return min.map(v -> ComparisonCellValue.of(v + " sq.ft", v)).orElse(ComparisonCellValue.missing());
        });

        addOptionalTextRow(rows, "maxSuperArea", "Max Super Area", "TEXT", false, projects, p -> {
            Optional<BigDecimal> max = floorPlansMap.getOrDefault(p.getId(), List.of()).stream()
                    .map(ProjectFloorPlanEntity::getSuperAreaSqft).filter(Objects::nonNull).max(Comparator.naturalOrder());
            return max.map(v -> ComparisonCellValue.of(v + " sq.ft", v)).orElse(ComparisonCellValue.missing());
        });

        return section(ComparisonSectionKey.UNITS, rows);
    }

    public ComparisonSection buildAmenities(
            List<ProjectEntity> projects,
            Map<Long, List<ProjectAmenityProgressEntity>> amenitiesMap,
            Map<Long, ProjectMeterSnapshotEntity> snapshots
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        addOptionalTextRow(rows, "amenityCompletionPercent", "Amenity Completion", "PERCENT", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            return s != null && s.getAmenityScore() != null
                    ? ComparisonCellValue.of(s.getAmenityScore() + "%", s.getAmenityScore())
                    : ComparisonCellValue.missing();
        });

        // Step 1: Build canonical code → label map (first-seen wins)
        Map<String, String> canonicalToLabel = new LinkedHashMap<>();
        projects.forEach(p -> amenitiesMap.getOrDefault(p.getId(), List.of()).forEach(a -> {
            String canonical = normalizeAmenityCode(a.getAmenityCode());
            canonicalToLabel.putIfAbsent(canonical, a.getAmenityLabel());
        }));

        // Step 2: Build best-entity lookup: projectId → (canonicalCode → best entity)
        Map<Long, Map<String, ProjectAmenityProgressEntity>> bestEntityLookup = new LinkedHashMap<>();
        for (ProjectEntity p : projects) {
            Map<String, ProjectAmenityProgressEntity> bestByCode = new LinkedHashMap<>();
            for (ProjectAmenityProgressEntity a : amenitiesMap.getOrDefault(p.getId(), List.of())) {
                String canonical = normalizeAmenityCode(a.getAmenityCode());
                ProjectAmenityProgressEntity existing = bestByCode.get(canonical);
                if (existing == null || isBetterAmenity(a, existing)) {
                    bestByCode.put(canonical, a);
                }
            }
            bestEntityLookup.put(p.getId(), bestByCode);
        }

        // Step 3: Collect all canonical codes seen, in priority order
        Set<String> allCanonicalCodes = new LinkedHashSet<>();
        for (Map<String, ProjectAmenityProgressEntity> byCode : bestEntityLookup.values()) {
            allCanonicalCodes.addAll(byCode.keySet());
        }
        List<String> sortedCodes = sortAmenityCodes(allCanonicalCodes);

        // Step 4: One row per canonical code (skip if all projects missing this amenity)
        for (String canonical : sortedCodes) {
            Map<String, ComparisonCellValue> values = new LinkedHashMap<>();
            boolean anyPresent = false;
            for (ProjectEntity p : projects) {
                ProjectAmenityProgressEntity best = bestEntityLookup.getOrDefault(p.getId(), Map.of()).get(canonical);
                if (best != null) {
                    anyPresent = true;
                    values.put(String.valueOf(p.getId()),
                            ComparisonCellValue.of(formatAmenityStatus(best.getStatus()), best.getStatus().name()));
                } else {
                    values.put(String.valueOf(p.getId()), ComparisonCellValue.missing());
                }
            }
            if (anyPresent) {
                String label = canonicalToLabel.getOrDefault(canonical, canonical);
                rows.add(ComparisonRow.builder()
                        .rowKey("amenity_" + canonical)
                        .label(label)
                        .valueType("STATUS_BADGE")
                        .highlight(false)
                        .values(values)
                        .build());
            }
        }

        return section(ComparisonSectionKey.AMENITIES, rows);
    }

    public ComparisonSection buildLocation(
            List<ProjectEntity> projects,
            Map<Long, ProjectLocationScoreEntity> locationScores,
            Map<Long, List<ProjectConnectivityPlaceEntity>> connectivityMap
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        rows.add(textRow("finalLocationScore", "Location Score", "SCORE", true, projects, p -> {
            ProjectLocationScoreEntity ls = locationScores.get(p.getId());
            return ls != null && ls.getFinalScore() != null
                    ? ComparisonCellValue.of(formatScore10(ls.getFinalScore()), ls.getFinalScore())
                    : ComparisonCellValue.missing();
        }));

        addOptionalTextRow(rows, "metroScore", "Metro Score", "SCORE", false, projects, p -> {
            ProjectLocationScoreEntity ls = locationScores.get(p.getId());
            return ls != null && ls.getMetroScore() != null
                    ? ComparisonCellValue.of(formatScore10(ls.getMetroScore()), ls.getMetroScore())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "educationScore", "Education Score", "SCORE", false, projects, p -> {
            ProjectLocationScoreEntity ls = locationScores.get(p.getId());
            return ls != null && ls.getEducationScore() != null
                    ? ComparisonCellValue.of(formatScore10(ls.getEducationScore()), ls.getEducationScore())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "healthcareScore", "Healthcare Score", "SCORE", false, projects, p -> {
            ProjectLocationScoreEntity ls = locationScores.get(p.getId());
            return ls != null && ls.getHealthcareScore() != null
                    ? ComparisonCellValue.of(formatScore10(ls.getHealthcareScore()), ls.getHealthcareScore())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "retailScore", "Retail Score", "SCORE", false, projects, p -> {
            ProjectLocationScoreEntity ls = locationScores.get(p.getId());
            return ls != null && ls.getRetailScore() != null
                    ? ComparisonCellValue.of(formatScore10(ls.getRetailScore()), ls.getRetailScore())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "jobScore", "Job Hub Score", "SCORE", false, projects, p -> {
            ProjectLocationScoreEntity ls = locationScores.get(p.getId());
            return ls != null && ls.getJobScore() != null
                    ? ComparisonCellValue.of(formatScore10(ls.getJobScore()), ls.getJobScore())
                    : ComparisonCellValue.missing();
        });

        addNearestPlaceRow(rows, "nearestMetro",    "Nearest Metro",    ProjectConnectivityCategory.TRANSIT,   projects, connectivityMap);
        addNearestPlaceRow(rows, "nearestHospital", "Nearest Hospital", ProjectConnectivityCategory.HOSPITALS, projects, connectivityMap);
        addNearestPlaceRow(rows, "nearestSchool",   "Nearest School",   ProjectConnectivityCategory.SCHOOLS,   projects, connectivityMap);
        addNearestPlaceRow(rows, "nearestMall",     "Nearest Mall",     ProjectConnectivityCategory.MALLS,     projects, connectivityMap);

        return section(ComparisonSectionKey.LOCATION, rows);
    }

    public ComparisonSection buildConstruction(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots,
            Map<Long, List<ProjectConstructionStageEntity>> stagesMap
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        addOptionalTextRow(rows, "constructionProgress", "Construction Progress", "PERCENT", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            return s != null && s.getConstructionProgressPercent() != null
                    ? ComparisonCellValue.of(s.getConstructionProgressPercent() + "%", s.getConstructionProgressPercent())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "delayDays", "Delay (days)", "TEXT", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            if (s == null || s.getDelayDays() == null) return ComparisonCellValue.missing();
            int delay = Math.max(s.getDelayDays(), 0);
            return delay == 0
                    ? ComparisonCellValue.of("On Track", 0)
                    : ComparisonCellValue.of(delay + " days delayed", delay);
        });

        rows.add(textRow("possessionDate", "Possession Date", "DATE", false, projects, p ->
                cv(formatDate(p.getPossessionDate()))));

        addOptionalTextRow(rows, "verifiedStageCount", "Verified Stages", "TEXT", false, projects, p -> {
            long count = stagesMap.getOrDefault(p.getId(), List.of()).stream()
                    .filter(s -> Boolean.TRUE.equals(s.getVerified())).count();
            int total = stagesMap.getOrDefault(p.getId(), List.of()).size();
            return total > 0
                    ? ComparisonCellValue.of(count + "/" + total + " verified", count)
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "delayedStages", "Delayed Stages", "TEXT", false, projects, p -> {
            long delayed = stagesMap.getOrDefault(p.getId(), List.of()).stream()
                    .filter(s -> s.getStatus() == ProjectStageStatus.DELAYED).count();
            return stagesMap.getOrDefault(p.getId(), List.of()).isEmpty()
                    ? ComparisonCellValue.missing()
                    : ComparisonCellValue.of(delayed == 0 ? "None" : delayed + " delayed", delayed);
        });

        return section(ComparisonSectionKey.CONSTRUCTION, rows);
    }

    public ComparisonSection buildCompliance(
            List<ProjectEntity> projects,
            Map<Long, List<ProjectComplianceItemEntity>> complianceMap
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        rows.add(textRow("reraStatus", "RERA Number", "TEXT", false, projects, p -> cv(p.getReraNumber())));

        addOptionalTextRow(rows, "approvalsVerifiedCount", "Verified Approvals", "TEXT", false, projects, p -> {
            List<ProjectComplianceItemEntity> items = complianceMap.getOrDefault(p.getId(), List.of());
            long verified = items.stream().filter(i -> Boolean.TRUE.equals(i.getVerified())).count();
            return items.isEmpty()
                    ? ComparisonCellValue.missing()
                    : ComparisonCellValue.of(verified + "/" + items.size() + " verified", verified);
        });

        // Per compliance item key rows
        Set<String> allKeys = projects.stream()
                .flatMap(p -> complianceMap.getOrDefault(p.getId(), List.of()).stream())
                .map(ProjectComplianceItemEntity::getItemKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> keyToLabel = new LinkedHashMap<>();
        projects.forEach(p -> complianceMap.getOrDefault(p.getId(), List.of()).forEach(c -> {
            if (!keyToLabel.containsKey(c.getItemKey())) {
                keyToLabel.put(c.getItemKey(), c.getItemLabel());
            }
        }));

        Map<Long, Map<String, ProjectComplianceStatus>> complianceLookup = new HashMap<>();
        projects.forEach(p -> {
            Map<String, ProjectComplianceStatus> byKey = complianceMap.getOrDefault(p.getId(), List.of())
                    .stream()
                    .collect(Collectors.toMap(
                            ProjectComplianceItemEntity::getItemKey,
                            ProjectComplianceItemEntity::getStatus,
                            (a, b) -> a
                    ));
            complianceLookup.put(p.getId(), byKey);
        });

        for (String key : allKeys) {
            String label = keyToLabel.getOrDefault(key, key);
            Map<String, ComparisonCellValue> values = new LinkedHashMap<>();
            for (ProjectEntity p : projects) {
                ProjectComplianceStatus status = complianceLookup.getOrDefault(p.getId(), Map.of()).get(key);
                values.put(String.valueOf(p.getId()), status != null
                        ? ComparisonCellValue.of(formatComplianceStatus(status), status.name())
                        : ComparisonCellValue.missing());
            }
            rows.add(ComparisonRow.builder()
                    .rowKey("compliance_" + key.toLowerCase())
                    .label(label)
                    .valueType("BADGE")
                    .highlight(false)
                    .values(values)
                    .build());
        }

        return section(ComparisonSectionKey.COMPLIANCE, rows);
    }

    public ComparisonSection buildBuilder(
            List<ProjectEntity> projects,
            Map<Long, BuilderCredibilitySummaryResponse> credibilityMap
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        rows.add(textRow("builderName", "Builder", "TEXT", false, projects, p ->
                cv(p.getBuilder() != null ? p.getBuilder().getName() : null)));

        addOptionalTextRow(rows, "builderScore", "Credibility Score", "SCORE", true, projects, p -> {
            BuilderCredibilitySummaryResponse cred = p.getBuilder() != null
                    ? credibilityMap.get(p.getBuilder().getId()) : null;
            return cred != null && cred.getCredibilityScore() != null
                    ? ComparisonCellValue.of(cred.getCredibilityScore() + "/100", cred.getCredibilityScore())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "credibilityLabel", "Credibility Label", "BADGE", false, projects, p -> {
            BuilderCredibilitySummaryResponse cred = p.getBuilder() != null
                    ? credibilityMap.get(p.getBuilder().getId()) : null;
            return cred != null ? cv(cred.getCredibilityLabel()) : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "projectsTracked", "Projects Tracked", "TEXT", false, projects, p -> {
            BuilderCredibilitySummaryResponse cred = p.getBuilder() != null
                    ? credibilityMap.get(p.getBuilder().getId()) : null;
            return cred != null && cred.getProjectsTrackedCount() != null
                    ? ComparisonCellValue.of(String.valueOf(cred.getProjectsTrackedCount()), cred.getProjectsTrackedCount())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "onTimeRecord", "On-Track Record", "PERCENT", false, projects, p -> {
            BuilderCredibilitySummaryResponse cred = p.getBuilder() != null
                    ? credibilityMap.get(p.getBuilder().getId()) : null;
            return cred != null && cred.getOnTrackRecordPercent() != null
                    ? ComparisonCellValue.of(formatPercent(cred.getOnTrackRecordPercent()), cred.getOnTrackRecordPercent())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "promisesMet", "Promises Met", "PERCENT", false, projects, p -> {
            BuilderCredibilitySummaryResponse cred = p.getBuilder() != null
                    ? credibilityMap.get(p.getBuilder().getId()) : null;
            return cred != null && cred.getPromisesMetPercent() != null
                    ? ComparisonCellValue.of(formatPercent(cred.getPromisesMetPercent()), cred.getPromisesMetPercent())
                    : ComparisonCellValue.missing();
        });

        // Note when all compared projects share the same builder
        long uniqueBuilders = projects.stream()
                .filter(p -> p.getBuilder() != null)
                .map(p -> p.getBuilder().getId())
                .distinct().count();
        String description = null;
        if (uniqueBuilders == 1 && projects.stream().allMatch(p -> p.getBuilder() != null)) {
            description = "All compared projects are by " + projects.get(0).getBuilder().getName();
        }

        return section(ComparisonSectionKey.BUILDER, rows, description);
    }

    /**
     * Compliance score is computed from actual compliance items (verified / total * 100)
     * rather than trusting the snapshot's cached integer which may store a count, not a percentage.
     */
    public ComparisonSection buildMeter(
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots,
            Map<Long, ProjectLocationScoreEntity> locationScores,
            Map<Long, List<ProjectComplianceItemEntity>> complianceMap
    ) {
        List<ComparisonRow> rows = new ArrayList<>();

        rows.add(buildScoreRow("meterScore", "Meter Score", projects, snapshots));

        addOptionalTextRow(rows, "locationScore", "Location Score", "SCORE", false, projects, p -> {
            ProjectLocationScoreEntity ls = locationScores.get(p.getId());
            return ls != null && ls.getFinalScore() != null
                    ? ComparisonCellValue.of(formatScore10(ls.getFinalScore()), ls.getFinalScore())
                    : ComparisonCellValue.missing();
        });

        addOptionalTextRow(rows, "constructionProgress", "Construction Progress", "PERCENT", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            return s != null && s.getConstructionProgressPercent() != null
                    ? ComparisonCellValue.of(s.getConstructionProgressPercent() + "%", s.getConstructionProgressPercent())
                    : ComparisonCellValue.missing();
        });

        // Compute compliance score from items (verified / total * 100), not snapshot integer
        addOptionalTextRow(rows, "complianceScore", "Compliance Score", "PERCENT", false, projects, p -> {
            List<ProjectComplianceItemEntity> items = complianceMap.getOrDefault(p.getId(), List.of());
            if (items.isEmpty()) return ComparisonCellValue.missing();
            long verified = items.stream().filter(i -> Boolean.TRUE.equals(i.getVerified())).count();
            int pct = (int) Math.round(verified * 100.0 / items.size());
            return ComparisonCellValue.of(pct + "%", pct);
        });

        addOptionalTextRow(rows, "verified", "Meter Verified", "BOOLEAN", false, projects, p -> {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            if (s == null) return ComparisonCellValue.missing();
            boolean v = Boolean.TRUE.equals(s.getVerified());
            return ComparisonCellValue.of(v ? "YES" : "NO", v);
        });

        return section(ComparisonSectionKey.METER, rows);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /** Adds a row only when at least one project has available data. */
    private void addOptionalTextRow(
            List<ComparisonRow> rows,
            String rowKey, String label, String valueType, boolean highlight,
            List<ProjectEntity> projects, CellSupplier supplier
    ) {
        ComparisonRow row = textRow(rowKey, label, valueType, highlight, projects, supplier);
        boolean anyAvailable = row.getValues().values().stream().anyMatch(ComparisonCellValue::isAvailable);
        if (anyAvailable) rows.add(row);
    }

    private ComparisonRow buildScoreRow(
            String rowKey,
            String label,
            List<ProjectEntity> projects,
            Map<Long, ProjectMeterSnapshotEntity> snapshots
    ) {
        Map<String, ComparisonCellValue> values = new LinkedHashMap<>();
        for (ProjectEntity p : projects) {
            ProjectMeterSnapshotEntity s = snapshots.get(p.getId());
            values.put(String.valueOf(p.getId()), s != null && s.getLocationScore() != null
                    ? ComparisonCellValue.of(formatScore10(s.getLocationScore()), s.getLocationScore())
                    : ComparisonCellValue.missing());
        }
        return ComparisonRow.builder()
                .rowKey(rowKey).label(label).valueType("SCORE").highlight(true).values(values).build();
    }

    private void addNearestPlaceRow(
            List<ComparisonRow> rows,
            String rowKey, String label,
            ProjectConnectivityCategory category,
            List<ProjectEntity> projects,
            Map<Long, List<ProjectConnectivityPlaceEntity>> connectivityMap
    ) {
        Map<String, ComparisonCellValue> values = new LinkedHashMap<>();
        boolean anyData = false;
        for (ProjectEntity p : projects) {
            Optional<ProjectConnectivityPlaceEntity> nearest = connectivityMap
                    .getOrDefault(p.getId(), List.of()).stream()
                    .filter(pl -> category.equals(pl.getCategory()))
                    .min(Comparator.comparingInt(pl -> pl.getDistanceMeters() != null ? pl.getDistanceMeters() : Integer.MAX_VALUE));
            if (nearest.isPresent()) {
                anyData = true;
                ProjectConnectivityPlaceEntity pl = nearest.get();
                String display = pl.getPlaceName();
                if (pl.getDistanceMeters() != null) {
                    display += " (" + formatDistanceMeters(pl.getDistanceMeters()) + ")";
                } else if (pl.getDistanceLabel() != null) {
                    display += " (" + pl.getDistanceLabel() + ")";
                }
                values.put(String.valueOf(p.getId()), ComparisonCellValue.of(display, pl.getDistanceMeters()));
            } else {
                values.put(String.valueOf(p.getId()), ComparisonCellValue.missing());
            }
        }
        if (anyData) {
            rows.add(ComparisonRow.builder()
                    .rowKey(rowKey).label(label).valueType("TEXT").highlight(false).values(values).build());
        }
    }

    /** Returns the canonical amenity code, normalizing known aliases. */
    private String normalizeAmenityCode(String code) {
        if (code == null) return "";
        String lower = code.toLowerCase().trim();
        return AMENITY_ALIASES.getOrDefault(lower, lower);
    }

    /**
     * Returns true when candidate is a better choice than existing for the same project/canonical code.
     * Priority: higher STATUS_PRIORITY > verified=true > lower displayOrder > lower id.
     */
    private boolean isBetterAmenity(ProjectAmenityProgressEntity candidate, ProjectAmenityProgressEntity existing) {
        int cp = STATUS_PRIORITY.getOrDefault(candidate.getStatus(), 0);
        int ep = STATUS_PRIORITY.getOrDefault(existing.getStatus(), 0);
        if (cp != ep) return cp > ep;
        boolean cv = Boolean.TRUE.equals(candidate.getVerified());
        boolean ev = Boolean.TRUE.equals(existing.getVerified());
        if (cv != ev) return cv;
        int co = candidate.getDisplayOrder() != null ? candidate.getDisplayOrder() : Integer.MAX_VALUE;
        int eo = existing.getDisplayOrder() != null ? existing.getDisplayOrder() : Integer.MAX_VALUE;
        if (co != eo) return co < eo;
        return candidate.getId() != null && existing.getId() != null && candidate.getId() < existing.getId();
    }

    /** Sorts amenity codes: PRIORITY_AMENITY_CODES first (in that list's order), then the rest in DB order. */
    private List<String> sortAmenityCodes(Set<String> codes) {
        List<String> result = new ArrayList<>();
        for (String priority : PRIORITY_AMENITY_CODES) {
            if (codes.contains(priority)) result.add(priority);
        }
        for (String code : codes) {
            if (!PRIORITY_AMENITY_CODES.contains(code)) result.add(code);
        }
        return result;
    }

    private interface CellSupplier {
        ComparisonCellValue get(ProjectEntity project);
    }

    private ComparisonRow textRow(
            String rowKey, String label, String valueType, boolean highlight,
            List<ProjectEntity> projects, CellSupplier supplier
    ) {
        Map<String, ComparisonCellValue> values = new LinkedHashMap<>();
        for (ProjectEntity p : projects) {
            values.put(String.valueOf(p.getId()), supplier.get(p));
        }
        return ComparisonRow.builder()
                .rowKey(rowKey).label(label).valueType(valueType).highlight(highlight).values(values).build();
    }

    private ComparisonCellValue cv(String value) {
        return (value != null && !value.isBlank())
                ? ComparisonCellValue.of(value, value)
                : ComparisonCellValue.missing();
    }

    private ComparisonSection section(ComparisonSectionKey key, List<ComparisonRow> rows) {
        return section(key, rows, null);
    }

    private ComparisonSection section(ComparisonSectionKey key, List<ComparisonRow> rows, String description) {
        return ComparisonSection.builder()
                .sectionKey(key)
                .sectionTitle(key.toTitle())
                .displayOrder(key.defaultOrder())
                .initiallyExpanded(key.initiallyExpanded())
                .description(description)
                .rows(rows)
                .build();
    }

    // ─── Formatting utilities ─────────────────────────────────────────────────

    private String formatPriceRange(Long min, Long max) {
        if (min == null && max == null) return "—";
        if (min == null) return formatPrice(max);
        if (max == null) return formatPrice(min);
        if (min.equals(max)) return formatPrice(min);
        return formatPrice(min) + " – " + formatPrice(max);
    }

    private String formatPrice(Long amount) {
        if (amount == null) return "—";
        if (amount >= CRORE) {
            double cr = amount / (double) CRORE;
            return "₹" + formatDecimal(cr) + " Cr";
        }
        if (amount >= LAKH) {
            double lakh = amount / (double) LAKH;
            return "₹" + formatDecimal(lakh) + " L";
        }
        return "₹" + formatNumber(amount);
    }

    private String formatDecimal(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.format("%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String formatNumber(Long value) {
        if (value == null) return "—";
        return String.format("%,d", value);
    }

    private String formatPercent(Double value) {
        if (value == null) return "—";
        return String.format("%.1f", value) + "%";
    }

    private String formatScore10(Double value) {
        if (value == null) return "—";
        return String.format("%.1f", value).replaceAll("\\.0$", "") + "/10";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(MONTH_YEAR) : null;
    }

    private String formatDistanceMeters(Integer meters) {
        if (meters == null) return "";
        if (meters >= 1000) return String.format("%.1f km", meters / 1000.0);
        return meters + " m";
    }

    private String formatAmenityStatus(ProjectAmenityStatus status) {
        return switch (status) {
            case COMPLETED     -> "Completed";
            case IN_PROGRESS   -> "In Progress";
            case PLANNED       -> "Planned";
            case NOT_STARTED   -> "Planned";
            case NOT_AVAILABLE -> "Not Available";
        };
    }

    private String formatComplianceStatus(ProjectComplianceStatus status) {
        return switch (status) {
            case OBTAINED, VERIFIED, APPROVED -> "Obtained";
            case SUBMITTED                    -> "Submitted";
            case PENDING                      -> "Pending";
            case NOT_APPLICABLE               -> "Not Applicable";
            case EXPIRED                      -> "Expired";
        };
    }

    private String formatStatus(String statusName) {
        return switch (statusName) {
            case "UPCOMING"           -> "Upcoming";
            case "UNDER_CONSTRUCTION" -> "Under Construction";
            case "READY_TO_MOVE"      -> "Ready to Move";
            case "COMPLETED"          -> "Completed";
            default                   -> statusName;
        };
    }

    private String formatPropertyTypes(Set<?> types) {
        if (types == null || types.isEmpty()) return null;
        return types.stream().map(t -> formatStatus(t.toString())).sorted().collect(Collectors.joining(", "));
    }
}
