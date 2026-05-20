package com.brandPitara.sfs.dashboard.scraping.candidate.mapper;

import com.brandPitara.sfs.dashboard.scraping.candidate.dto.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.entity.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateDocumentType;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.dto.*;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Converts a {@link ReraNumberSearchResponse} into a graph of scrape candidate entities
 * ready to be persisted. Also derives cost breakdown, land utilization, and plan/utility
 * compliance items from raw key-value pairs found in the RERA source data.
 *
 * The returned {@link CandidateEntityGraph} contains entities WITHOUT their candidate
 * reference set. The calling service sets the reference after saving the main candidate.
 */
@Slf4j
@Component
public class ScrapeCandidateEntityMapper {

    // ── Area conversion ──────────────────────────────────────────────────────
    private static final double ACRE_TO_SQM    = 4046.8564224;
    private static final double HECTARE_TO_SQM = 10_000.0;

    // ── Land area raw keys ────────────────────────────────────────────────────
    private static final List<String> LAND_AREA_KEYS = List.of(
            "1landareaoftheproject",
            "landareaoftheproject",
            "itotalareaoftheproject",
            "totallicensedarea",
            "4totallicensedareaifthelandareaofthepresentprojectisapartthereof"
    );

    // ── Plan / approval compliance keys (Yes/No from Form A-H) ───────────────
    private record PlanMapping(ProjectComplianceGroup group, String key, String label, int order) {}

    private static final List<PlanMapping> PLAN_COMPLIANCE_MAPPINGS = List.of(
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "LAYOUT_PLAN",           "Layout Plan",                   10),
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "DEMARCATION_PLAN",      "Demarcation Plan",              11),
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "ZONING_PLAN",           "Zoning Plan",                   12),
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "BUILDING_PLAN",         "Building Plan",                 13),
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "SITE_PLAN",             "Site Plan",                     14),
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "FLOOR_PLAN_DOCUMENT",   "Floor Plan (Document)",         15),
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "APARTMENT_PLAN",        "Apartment Plans",               16),
            new PlanMapping(ProjectComplianceGroup.APPROVAL_NOC, "ROAD_PAVEMENT_PLAN",    "Roads and Pavement Plan",       17),
            new PlanMapping(ProjectComplianceGroup.UTILITY,      "ELECTRICITY_SUPPLY",    "Electricity Supply Plan",       20),
            new PlanMapping(ProjectComplianceGroup.UTILITY,      "WATER_SUPPLY",          "Water Supply Plan",             21),
            new PlanMapping(ProjectComplianceGroup.UTILITY,      "SEWERAGE_GARBAGE",      "Sewerage and Garbage Disposal", 22),
            new PlanMapping(ProjectComplianceGroup.UTILITY,      "STORM_WATER_DRAINAGE",  "Storm Water Drainage",          23),
            new PlanMapping(ProjectComplianceGroup.UTILITY,      "PARKING_PLAN",          "Parking Plan",                  24)
    );

    private static final Map<String, PlanMapping> PLAN_KEY_TO_MAPPING = buildPlanKeyMap();

    private static Map<String, PlanMapping> buildPlanKeyMap() {
        Map<String, PlanMapping> map = new LinkedHashMap<>();
        String[] rawKeys = {
                "ilayoutplan",
                "iidemarcationplan",
                "iiizoningplan",
                "ivbuildingplan",
                "siteplan",
                "floorplan",
                "apartmentplans",
                "iroadsandpavementplan",
                "iielectricitysupplyplan",
                "iiiwatersupplyplan",
                "ivsewerageandgarbagedisposalplan",
                "vstormwaterdrainage",
                "viiiparkingplan"
        };
        for (int i = 0; i < rawKeys.length && i < PLAN_COMPLIANCE_MAPPINGS.size(); i++) {
            map.put(rawKeys[i], PLAN_COMPLIANCE_MAPPINGS.get(i));
        }
        return Collections.unmodifiableMap(map);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public record CandidateEntityGraph(
            DashboardScrapeCandidateEntity candidate,
            DashboardScrapeCandidateProjectEntity project,
            DashboardScrapeCandidateBuilderEntity builder,
            List<DashboardScrapeCandidateComplianceItemEntity> complianceItems,
            List<DashboardScrapeCandidateFieldResultEntity> fieldResults,
            List<DashboardScrapeCandidateRawValueEntity> rawValues,
            DashboardScrapeCandidateCostBreakdownEntity costBreakdown,
            DashboardScrapeCandidateLandUtilizationEntity landUtilization,
            List<DashboardScrapeCandidateDocumentEntity> documents
    ) {}

    public CandidateEntityGraph toEntityGraph(ReraNumberSearchResponse response,
                                               Long createdByUserId) {
        ScrapeCandidateStatus status = resolveInitialStatus(response);

        DashboardScrapeCandidateEntity candidate = buildCandidate(response, status, createdByUserId);
        DashboardScrapeCandidateProjectEntity project = buildProject(response);
        DashboardScrapeCandidateBuilderEntity builder = buildBuilder(response);

        Map<String, String> rawKv = extractRawKv(response);

        List<DashboardScrapeCandidateComplianceItemEntity> complianceItems =
                buildComplianceItems(response, rawKv);
        List<DashboardScrapeCandidateFieldResultEntity> fieldResults =
                buildFieldResults(response);
        List<DashboardScrapeCandidateRawValueEntity> rawValues =
                buildRawValues(rawKv);
        DashboardScrapeCandidateCostBreakdownEntity costBreakdown =
                deriveCostBreakdown(rawKv);
        DashboardScrapeCandidateLandUtilizationEntity landUtilization =
                deriveLandUtilization(rawKv);
        List<DashboardScrapeCandidateDocumentEntity> documents =
                buildDocuments(response);

        return new CandidateEntityGraph(candidate, project, builder, complianceItems,
                fieldResults, rawValues, costBreakdown, landUtilization, documents);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Entity to DTO converters (for detail response assembly)
    // ─────────────────────────────────────────────────────────────────────────

    public ScrapeCandidateProjectDto toProjectDto(DashboardScrapeCandidateProjectEntity e) {
        if (e == null) return null;
        return ScrapeCandidateProjectDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .cityName(e.getCityName())
                .addressLine(e.getAddressLine())
                .latitude(e.getLatitude() != null ? e.getLatitude().doubleValue() : null)
                .longitude(e.getLongitude() != null ? e.getLongitude().doubleValue() : null)
                .priceMin(e.getPriceMin())
                .priceMax(e.getPriceMax())
                .possessionDate(e.getPossessionDate())
                .reraNumber(e.getReraNumber())
                .projectStatus(e.getProjectStatus())
                .propertyTypes(splitPropertyTypes(e.getPropertyTypes()))
                .build();
    }

    public ScrapeCandidateBuilderDto toBuilderDto(DashboardScrapeCandidateBuilderEntity e) {
        if (e == null) return null;
        return ScrapeCandidateBuilderDto.builder()
                .id(e.getId())
                .name(e.getName())
                .phone(e.getPhone())
                .email(e.getEmail())
                .addressLine(e.getAddressLine())
                .cityName(e.getCityName())
                .website(e.getWebsite())
                .build();
    }

    public ScrapeCandidateComplianceItemDto toComplianceDto(DashboardScrapeCandidateComplianceItemEntity e) {
        return ScrapeCandidateComplianceItemDto.builder()
                .id(e.getId())
                .itemGroup(e.getItemGroup())
                .itemKey(e.getItemKey())
                .itemLabel(e.getItemLabel())
                .status(e.getStatus())
                .valueText(e.getValueText())
                .documentUrl(e.getDocumentUrl())
                .remarks(e.getRemarks())
                .displayOrder(e.getDisplayOrder())
                .verified(e.isVerified())
                .build();
    }

    public ScrapeCandidateFieldResultDto toFieldResultDto(DashboardScrapeCandidateFieldResultEntity e) {
        return ScrapeCandidateFieldResultDto.builder()
                .id(e.getId())
                .section(e.getSection())
                .fieldKey(e.getFieldKey())
                .fieldLabel(e.getFieldLabel())
                .found(e.isFound())
                .valueText(e.getValueText())
                .sourceLabel(e.getSourceLabel())
                .confidence(e.getConfidence())
                .reason(e.getReason())
                .build();
    }

    public ScrapeCandidateRawValueDto toRawValueDto(DashboardScrapeCandidateRawValueEntity e) {
        return ScrapeCandidateRawValueDto.builder()
                .id(e.getId())
                .rawKey(e.getRawKey())
                .rawValue(e.getRawValue())
                .build();
    }

    public ScrapeCandidateCostBreakdownDto toCostBreakdownDto(DashboardScrapeCandidateCostBreakdownEntity e) {
        if (e == null) return null;
        return ScrapeCandidateCostBreakdownDto.builder()
                .id(e.getId())
                .landCost(e.getLandCost())
                .constructionCost(e.getConstructionCost())
                .infrastructureCost(e.getInfrastructureCost())
                .otherCost(e.getOtherCost())
                .totalCost(e.getTotalCost())
                .sourceLabel(e.getSourceLabel())
                .remarks(e.getRemarks())
                .verified(e.isVerified())
                .build();
    }

    public ScrapeCandidateLandUtilizationDto toLandUtilizationDto(DashboardScrapeCandidateLandUtilizationEntity e) {
        if (e == null) return null;
        return ScrapeCandidateLandUtilizationDto.builder()
                .id(e.getId())
                .totalLandAreaSqm(e.getTotalLandAreaSqm() != null ? e.getTotalLandAreaSqm().doubleValue() : null)
                .residentialAreaSqm(e.getResidentialAreaSqm() != null ? e.getResidentialAreaSqm().doubleValue() : null)
                .commercialAreaSqm(e.getCommercialAreaSqm() != null ? e.getCommercialAreaSqm().doubleValue() : null)
                .parksAreaSqm(e.getParksAreaSqm() != null ? e.getParksAreaSqm().doubleValue() : null)
                .openAreaSqm(e.getOpenAreaSqm() != null ? e.getOpenAreaSqm().doubleValue() : null)
                .parkingAreaSqm(e.getParkingAreaSqm() != null ? e.getParkingAreaSqm().doubleValue() : null)
                .utilityAreaSqm(e.getUtilityAreaSqm() != null ? e.getUtilityAreaSqm().doubleValue() : null)
                .sourceLabel(e.getSourceLabel())
                .remarks(e.getRemarks())
                .verified(e.isVerified())
                .build();
    }

    public ScrapeCandidateDocumentDto toDocumentDto(DashboardScrapeCandidateDocumentEntity e) {
        return ScrapeCandidateDocumentDto.builder()
                .id(e.getId())
                .documentType(e.getDocumentType())
                .title(e.getTitle())
                .documentUrl(e.getDocumentUrl())
                .sourceLabel(e.getSourceLabel())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — entity builders
    // ─────────────────────────────────────────────────────────────────────────

    private DashboardScrapeCandidateEntity buildCandidate(ReraNumberSearchResponse r,
                                                           ScrapeCandidateStatus status,
                                                           Long userId) {
        ReraSearchSummaryDto summary = r.getSummary();
        String remarks = status == ScrapeCandidateStatus.CAPTCHA_REQUIRED
                ? "CAPTCHA required. Re-submit the same request with captchaText filled in."
                : null;
        return DashboardScrapeCandidateEntity.builder()
                .sourceCode(r.getSourceCode())
                .reraNumber(r.getReraNumber())
                .found(r.isFound())
                .captchaDetected(r.isCaptchaDetected())
                .captchaSessionId(r.getCaptchaSessionId())
                .sourceSearchUrl(r.getSourceSearchUrl())
                .sourceDetailUrl(r.getSourceDetailUrl())
                .finalUrl(r.getFinalUrl())
                .pageTitle(r.getTitle())
                .rawHtmlPath(r.getRawHtmlPath())
                .screenshotPath(r.getScreenshotPath())
                .confidenceScore(summary != null ? summary.getConfidenceScore() : null)
                .confidenceStatus(summary != null ? summary.getStatus() : null)
                .totalExpectedFields(summary != null ? summary.getTotalExpectedFields() : null)
                .foundFields(summary != null ? summary.getFoundFields() : null)
                .missingFields(summary != null ? summary.getMissingFields() : null)
                .status(status)
                .remarks(remarks)
                .createdByDashboardUserId(userId)
                .build();
    }

    private DashboardScrapeCandidateProjectEntity buildProject(ReraNumberSearchResponse r) {
        ScrapedProjectCandidateDto p = r.getProjectCandidate();
        if (p == null) return null;
        return DashboardScrapeCandidateProjectEntity.builder()
                .name(p.getName())
                .description(p.getDescription())
                .cityName(p.getCityName())
                .addressLine(p.getAddressLine())
                .latitude(p.getLatitude() != null ? BigDecimal.valueOf(p.getLatitude()) : null)
                .longitude(p.getLongitude() != null ? BigDecimal.valueOf(p.getLongitude()) : null)
                .priceMin(p.getPriceMin())
                .priceMax(p.getPriceMax())
                .possessionDate(p.getPossessionDate())
                .reraNumber(p.getReraNumber())
                .projectStatus(p.getStatus() != null ? p.getStatus().name() : null)
                .propertyTypes(joinPropertyTypes(p.getPropertyTypes()))
                .build();
    }

    private DashboardScrapeCandidateBuilderEntity buildBuilder(ReraNumberSearchResponse r) {
        ScrapedBuilderCandidateDto b = r.getBuilderCandidate();
        if (b == null) return null;
        return DashboardScrapeCandidateBuilderEntity.builder()
                .name(b.getName())
                .phone(b.getPhone())
                .email(b.getEmail())
                .addressLine(b.getAddressLine())
                .cityName(b.getCityName())
                .build();
    }

    private List<DashboardScrapeCandidateComplianceItemEntity> buildComplianceItems(
            ReraNumberSearchResponse r, Map<String, String> rawKv) {

        List<DashboardScrapeCandidateComplianceItemEntity> items = new ArrayList<>();

        // From mapped compliance candidates
        if (r.getComplianceCandidates() != null) {
            for (ScrapedComplianceCandidateDto c : r.getComplianceCandidates()) {
                items.add(DashboardScrapeCandidateComplianceItemEntity.builder()
                        .itemGroup(c.getItemGroup() != null ? c.getItemGroup().name() : null)
                        .itemKey(c.getItemKey())
                        .itemLabel(c.getItemLabel())
                        .status(c.getStatus() != null ? c.getStatus().name() : null)
                        .valueText(c.getValueText())
                        .documentUrl(c.getDocumentUrl())
                        .remarks(c.getRemarks())
                        .displayOrder(c.getDisplayOrder() != null ? c.getDisplayOrder() : 0)
                        .verified(Boolean.TRUE.equals(c.getVerified()))
                        .build());
            }
        }

        // Derive plan/utility compliance from Yes/No raw fields
        Set<String> existingKeys = new HashSet<>();
        items.forEach(i -> { if (i.getItemKey() != null) existingKeys.add(i.getItemKey()); });

        for (Map.Entry<String, PlanMapping> entry : PLAN_KEY_TO_MAPPING.entrySet()) {
            String rawValue = rawKv.getOrDefault(entry.getKey(), "");
            if (rawValue == null || rawValue.isBlank()) continue;

            PlanMapping pm = entry.getValue();
            if (existingKeys.contains(pm.key())) continue;

            ProjectComplianceStatus compStatus = isYesValue(rawValue)
                    ? ProjectComplianceStatus.OBTAINED
                    : ProjectComplianceStatus.PENDING;

            items.add(DashboardScrapeCandidateComplianceItemEntity.builder()
                    .itemGroup(pm.group().name())
                    .itemKey(pm.key())
                    .itemLabel(pm.label())
                    .status(compStatus.name())
                    .valueText(rawValue)
                    .displayOrder(pm.order())
                    .verified(false)
                    .build());
            existingKeys.add(pm.key());
        }

        return items;
    }

    private List<DashboardScrapeCandidateFieldResultEntity> buildFieldResults(
            ReraNumberSearchResponse r) {
        if (r.getFieldResults() == null) return List.of();
        return r.getFieldResults().stream()
                .map(f -> DashboardScrapeCandidateFieldResultEntity.builder()
                        .section(f.getSection() != null ? f.getSection().name() : null)
                        .fieldKey(f.getFieldKey())
                        .fieldLabel(f.getFieldLabel())
                        .found(f.isFound())
                        .valueText(f.getValue() != null ? f.getValue().toString() : null)
                        .sourceLabel(f.getSourceLabel())
                        .confidence(f.getConfidence())
                        .reason(f.getReason())
                        .build())
                .toList();
    }

    private List<DashboardScrapeCandidateRawValueEntity> buildRawValues(Map<String, String> rawKv) {
        return rawKv.entrySet().stream()
                .map(e -> DashboardScrapeCandidateRawValueEntity.builder()
                        .rawKey(e.getKey())
                        .rawValue(e.getValue())
                        .build())
                .toList();
    }

    private DashboardScrapeCandidateCostBreakdownEntity deriveCostBreakdown(Map<String, String> rawKv) {
        Long totalCost        = parseLakhsToRupees(rawKv.get("1estimatedcostoftheprojectannexacopyoftheprojectinfolderc"));
        Long landCost         = parseLakhsToRupees(rawKv.get("icostofthelandifincludedintheestimatedcost"));
        Long constructionCost = parseLakhsToRupees(rawKv.get("iiestimatedcostofconstructionofapartments"));
        Long infraCost        = parseLakhsToRupees(rawKv.get("iiiestimatedcostofinfrastructureandotherstructures"));
        Long otherCost        = parseLakhsToRupees(rawKv.get("ivothercostsincludingedctaxesleviesetc"));

        if (totalCost == null && landCost == null && constructionCost == null
                && infraCost == null && otherCost == null) {
            return null;
        }

        return DashboardScrapeCandidateCostBreakdownEntity.builder()
                .totalCost(totalCost)
                .landCost(landCost)
                .constructionCost(constructionCost)
                .infrastructureCost(infraCost)
                .otherCost(otherCost)
                .sourceLabel("RERA Form A-H")
                .verified(false)
                .build();
    }

    private DashboardScrapeCandidateLandUtilizationEntity deriveLandUtilization(Map<String, String> rawKv) {
        String rawArea = null;
        for (String key : LAND_AREA_KEYS) {
            rawArea = rawKv.get(key);
            if (rawArea != null && !rawArea.isBlank()) break;
        }
        if (rawArea == null) return null;

        BigDecimal sqm = parseAreaToSqm(rawArea);
        if (sqm == null) return null;

        return DashboardScrapeCandidateLandUtilizationEntity.builder()
                .totalLandAreaSqm(sqm)
                .sourceLabel("RERA Form A-H")
                .verified(false)
                .build();
    }

    private List<DashboardScrapeCandidateDocumentEntity> buildDocuments(ReraNumberSearchResponse r) {
        List<DashboardScrapeCandidateDocumentEntity> docs = new ArrayList<>();
        if (r.getSourceDetailUrl() != null && !r.getSourceDetailUrl().isBlank()) {
            docs.add(DashboardScrapeCandidateDocumentEntity.builder()
                    .documentType(ScrapeCandidateDocumentType.RERA_DETAIL_PAGE)
                    .title("RERA Project Detail Page")
                    .documentUrl(r.getSourceDetailUrl())
                    .sourceLabel(r.getSourceCode() != null ? r.getSourceCode().name() : "RERA")
                    .build());
        }
        return docs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — raw KV extraction
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, String> extractRawKv(ReraNumberSearchResponse r) {
        if (r.getRaw() == null) return Map.of();
        Object kv = r.getRaw().get("allExtractedKeyValues");
        if (kv instanceof Map<?, ?> map) {
            return (Map<String, String>) map;
        }
        return Map.of();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — value helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ScrapeCandidateStatus resolveInitialStatus(ReraNumberSearchResponse r) {
        if (r.isCaptchaDetected()) return ScrapeCandidateStatus.CAPTCHA_REQUIRED;
        if (!r.isFound()) return ScrapeCandidateStatus.FAILED;
        ReraSearchSummaryDto summary = r.getSummary();
        if (summary != null) {
            return summary.getConfidenceScore() >= 70
                    ? ScrapeCandidateStatus.READY_FOR_REVIEW
                    : ScrapeCandidateStatus.PARTIAL;
        }
        return ScrapeCandidateStatus.SCRAPED;
    }

    private boolean isYesValue(String v) {
        if (v == null) return false;
        String lower = v.trim().toLowerCase();
        return lower.equals("yes") || lower.equals("y") || lower.equals("1") || lower.equals("true");
    }

    private Long parseLakhsToRupees(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String numStr = raw.replaceAll("[^0-9.]", "");
        if (numStr.isBlank()) return null;
        try {
            return Math.round(Double.parseDouble(numStr) * 100_000.0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseAreaToSqm(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String numStr = raw.replaceAll("[^0-9.]", "");
        if (numStr.isBlank()) return null;
        try {
            double numeric = Double.parseDouble(numStr);
            String lower = raw.toLowerCase(Locale.ROOT);
            if (lower.contains("acre")) return round2(numeric * ACRE_TO_SQM);
            if (lower.contains("hectare") || lower.contains(" ha")) return round2(numeric * HECTARE_TO_SQM);
            return round2(numeric);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String joinPropertyTypes(Set<?> types) {
        if (types == null || types.isEmpty()) return null;
        return types.stream().map(Object::toString)
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    private List<String> splitPropertyTypes(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        return Arrays.asList(stored.split(","));
    }
}
