package com.brandPitara.sfs.dashboard.scraping.mapper;

import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchResponse;
import com.brandPitara.sfs.dashboard.scraping.dto.ReraSearchSummaryDto;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapedBuilderCandidateDto;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapedComplianceCandidateDto;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapedProjectCandidateDto;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeFieldResultDto;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeMissingFieldDto;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldConfidenceStatus;
import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldSection;
import com.brandPitara.sfs.dashboard.scraping.source.ReraRawSearchResult;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps a ReraRawSearchResult (generic key-value map from the HTML parser)
 * into typed candidate DTOs and a field-level confidence summary.
 *
 * Expected field definitions are declared here. The mapper looks up each field
 * using a synonym list, normalizes the raw value, and records whether it was found,
 * the confidence level, and the raw value.
 *
 * This class is source-agnostic — it works with any ReraSourceScraper output
 * as long as the key-value map uses normalized keys.
 */
@Slf4j
@Component
public class ReraProjectCandidateMapper {

    // -------------------------------------------------------------------------
    // Field definitions — every field we attempt to extract and report on
    // -------------------------------------------------------------------------

    private record FieldDef(
            ScrapeFieldSection section,
            String fieldKey,
            String fieldLabel,
            List<String> synonyms,
            int baseConfidence
    ) {}

    private static final List<FieldDef> EXPECTED_FIELDS = List.of(

            // PROJECT_BASIC
            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "name", "Project Name",
                    List.of("projectname", "nameoftheproject", "projecttitle", "schemename", "projectnames"), 90),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "cityName", "City / District",
                    List.of("city", "district", "districtname", "taluka", "projectdistrict"), 80),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "addressLine", "Project Address",
                    List.of("address", "projectaddress", "siteaddress", "projectlocation", "siteoflocation", "location"), 75),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "reraNumber", "RERA Registration Number",
                    List.of("reranumber", "registrationno", "registrationnumber", "reraregistrationno",
                            "projectregistrationno", "registrationid"), 100),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "status", "Project Status",
                    List.of("projectstatus", "status", "currentstatus", "statusofproject"), 80),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "possessionDate", "Possession / Completion Date",
                    List.of(
                            "proposedcompletiondate",
                            "completiondate",
                            "possessiondate",
                            "targetedcompletiondate",
                            "expectedcompletion",
                            "proposeddateofcompletion",
                            "projectcompletiondate",
                            "likelydateofcompletingtheproject",
                            "iilikelydateofcompletingtheproject",
                            "likelydateofcompletionoftheproject",
                            "initialdateofcompletionoftheproject",
                            "10initialdateofcompletionoftheproject",
                            "11likelydateofcompletionoftheproject"
                    ), 80),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "propertyTypes", "Property Type",
                    List.of(
                            "projecttype",
                            "propertytype",
                            "typeofproject",
                            "natureofproject",
                            "typeofproperty",
                            "nature",
                            "apartments",
                            "aapartments",
                            "itotalnumberofapartments",
                            "iitotalnumberofapartments",
                            "plots",
                            "bplots",
                            "iiitotalnumberofplots"
                    ), 75),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "description", "Project Description",
                    List.of("projectdescription", "description", "briefdescription", "about"), 60),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "latitude", "Latitude",
                    List.of("latitude", "lat"), 70),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "longitude", "Longitude",
                    List.of("longitude", "lng", "long"), 70),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "priceMin", "Minimum Price",
                    List.of("pricemin", "minimumprice", "startingprice", "pricerangefrom"), 60),

            new FieldDef(ScrapeFieldSection.PROJECT_BASIC, "priceMax", "Maximum Price",
                    List.of("pricemax", "maximumprice", "pricerangeto"), 60),

            // BUILDER
            new FieldDef(ScrapeFieldSection.BUILDER, "builderName", "Builder / Promoter Name",
                    List.of("promotername", "buildername", "developer", "applicantname",
                            "nameofpromoter", "promoter", "developernames", "nameofapplicant"), 90),

            new FieldDef(ScrapeFieldSection.BUILDER, "builderPhone", "Builder Phone",
                    List.of("contactnumber", "mobilenumber", "phone", "mobileno",
                            "contact", "promotercontact", "promotermobile",
                            "phonemobile", "phonelandline"), 80),

            new FieldDef(ScrapeFieldSection.BUILDER, "builderEmail", "Builder Email",
                    List.of("emailid", "email", "emailaddress", "promoteremail"), 80),

            new FieldDef(ScrapeFieldSection.BUILDER, "builderAddress", "Builder / Promoter Address",
                    List.of(
                            "promoteraddress",
                            "builderaddress",
                            "officeaddress",
                            "registeredaddress",
                            "annexacopyinfoldera",
                            "registeredaddressofthecompany",
                            "1nameandregisteredaddressofthecompany"
                    ), 70),

            new FieldDef(ScrapeFieldSection.BUILDER, "builderCity", "Builder City",
                    List.of("promotercity", "buildercity", "promoterdistrict"), 60),

            // PROJECT_METER_LAND_UTILIZATION
            new FieldDef(ScrapeFieldSection.PROJECT_METER_LAND_UTILIZATION, "totalLandAreaSqm", "Total Land Area (sqm)",
                    List.of(
                            "totallandarea",
                            "landarea",
                            "totalareainsqm",
                            "landareainsqm",
                            "plottedarea",
                            "totalplotarea",
                            "totalareaofland",
                            "landareaoftheproject",
                            "1landareaoftheproject",
                            "itotalareaoftheproject",
                            "totallicensedarea",
                            "4totallicensedareaifthelandareaofthepresentprojectisapartthereof"
                    ), 75)
    );

    private static final int TOTAL_EXPECTED = EXPECTED_FIELDS.size();

    private static final double ACRE_TO_SQM    = 4046.8564224;
    private static final double HECTARE_TO_SQM = 10_000.0;

    // -------------------------------------------------------------------------
    // Date formats tried in order during possessionDate parsing
    // -------------------------------------------------------------------------

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    // -------------------------------------------------------------------------
    // Main mapping entry point
    // -------------------------------------------------------------------------

    public ReraNumberSearchResponse map(ReraRawSearchResult raw,
                                        String reraNumber,
                                        ReraSourceCode sourceCode,
                                        boolean includeRaw) {

        Map<String, String> kv = raw.getExtractedKeyValues();

        List<ScrapeFieldResultDto> fieldResults  = new ArrayList<>();
        List<ScrapeMissingFieldDto> missingFields = new ArrayList<>();

        // Collected values for building candidates
        Map<String, String> found = new java.util.LinkedHashMap<>();

        for (FieldDef def : EXPECTED_FIELDS) {
            String rawValue = lookupFirst(kv, def.synonyms());

            if (rawValue != null) {
                found.put(def.fieldKey(), rawValue);
                fieldResults.add(ScrapeFieldResultDto.builder()
                        .section(def.section())
                        .fieldKey(def.fieldKey())
                        .fieldLabel(def.fieldLabel())
                        .found(true)
                        .value(normalizeValue(def.fieldKey(), rawValue))
                        .sourceLabel("RERA detail page")
                        .confidence(def.baseConfidence())
                        .build());
            } else {
                fieldResults.add(ScrapeFieldResultDto.builder()
                        .section(def.section())
                        .fieldKey(def.fieldKey())
                        .fieldLabel(def.fieldLabel())
                        .found(false)
                        .confidence(0)
                        .reason("Not found on RERA page")
                        .build());
                missingFields.add(ScrapeMissingFieldDto.builder()
                        .section(def.section())
                        .fieldKey(def.fieldKey())
                        .fieldLabel(def.fieldLabel())
                        .reason("Not found on RERA page")
                        .build());
            }
        }

        // Always inject the RERA number we searched for (100% confidence)
        injectReraNumberIfMissing(found, fieldResults, missingFields, reraNumber);

        ScrapedProjectCandidateDto project  = buildProjectCandidate(found, reraNumber);
        ScrapedBuilderCandidateDto builder  = buildBuilderCandidate(found);
        List<ScrapedComplianceCandidateDto> compliance = buildComplianceCandidates(found, reraNumber, raw.getSourceDetailUrl());

        ReraSearchSummaryDto summary = buildSummary(fieldResults, raw.isCaptchaDetected(), raw.isFound());

        Map<String, Object> rawPayload = null;
        if (includeRaw && kv != null) {
            rawPayload = Map.of("allExtractedKeyValues", kv);
        }

        return ReraNumberSearchResponse.builder()
                .sourceCode(sourceCode)
                .reraNumber(reraNumber)
                .found(raw.isFound())
                .captchaDetected(raw.isCaptchaDetected())
                .requestedAt(raw.getFetchedAt())
                .sourceSearchUrl(raw.getSourceSearchUrl())
                .sourceDetailUrl(raw.getSourceDetailUrl())
                .finalUrl(raw.getFinalUrl())
                .title(raw.getTitle())
                .summary(summary)
                .projectCandidate(project)
                .builderCandidate(builder)
                .complianceCandidates(compliance)
                .fieldResults(fieldResults)
                .missingFields(missingFields.isEmpty() ? null : missingFields)
                .warnings(raw.getWarnings().isEmpty() ? null : raw.getWarnings())
                .raw(rawPayload)
                .build();
    }

    // -------------------------------------------------------------------------
    // Candidate builders
    // -------------------------------------------------------------------------

    private ScrapedProjectCandidateDto buildProjectCandidate(Map<String, String> found,
                                                              String reraNumber) {
        String rawDate   = found.get("possessionDate");
        LocalDate possession = parseDate(rawDate);

        String rawStatus = found.get("status");
        ProjectStatus status = normalizeStatus(rawStatus);

        String rawType = found.get("propertyTypes");
        Set<PropertyType> types = normalizePropertyTypes(rawType);

        return ScrapedProjectCandidateDto.builder()
                .name(found.get("name"))
                .description(found.get("description"))
                .cityName(found.get("cityName"))
                .addressLine(found.get("addressLine"))
                .latitude(parseDouble(found.get("latitude")))
                .longitude(parseDouble(found.get("longitude")))
                .priceMin(parseLong(found.get("priceMin")))
                .priceMax(parseLong(found.get("priceMax")))
                .possessionDate(possession)
                .reraNumber(reraNumber)
                .status(status)
                .propertyTypes(types.isEmpty() ? null : types)
                .build();
    }

    private ScrapedBuilderCandidateDto buildBuilderCandidate(Map<String, String> found) {
        return ScrapedBuilderCandidateDto.builder()
                .name(found.get("builderName"))
                .phone(found.get("builderPhone"))
                .email(found.get("builderEmail"))
                .addressLine(found.get("builderAddress"))
                .cityName(found.get("builderCity"))
                .build();
    }

    private List<ScrapedComplianceCandidateDto> buildComplianceCandidates(
            Map<String, String> found, String reraNumber, String detailUrl) {

        List<ScrapedComplianceCandidateDto> list = new ArrayList<>();

        // Always produce RERA_REGISTRATION compliance item
        list.add(ScrapedComplianceCandidateDto.builder()
                .itemGroup(ProjectComplianceGroup.RERA)
                .itemKey("RERA_REGISTRATION")
                .itemLabel("RERA Registration")
                .status(ProjectComplianceStatus.OBTAINED)
                .valueText(reraNumber)
                .documentUrl(detailUrl)
                .remarks("Extracted from RERA portal")
                .displayOrder(1)
                .verified(false)
                .build());

        return list;
    }

    // -------------------------------------------------------------------------
    // Summary builder
    // -------------------------------------------------------------------------

    private ReraSearchSummaryDto buildSummary(List<ScrapeFieldResultDto> fieldResults,
                                               boolean captchaDetected, boolean found) {
        long foundCount = fieldResults.stream().filter(ScrapeFieldResultDto::isFound).count();
        long highConf   = fieldResults.stream()
                .filter(f -> f.isFound() && f.getConfidence() != null && f.getConfidence() >= 80)
                .count();
        long lowConf    = fieldResults.stream()
                .filter(f -> f.isFound() && f.getConfidence() != null && f.getConfidence() < 80)
                .count();

        int score = TOTAL_EXPECTED > 0
                ? (int) Math.round((foundCount * 100.0) / TOTAL_EXPECTED)
                : 0;

        ScrapeFieldConfidenceStatus status;
        if (captchaDetected) {
            status = ScrapeFieldConfidenceStatus.BLOCKED;
        } else if (!found || foundCount == 0) {
            status = ScrapeFieldConfidenceStatus.NOT_FOUND;
        } else if (score >= 85) {
            status = ScrapeFieldConfidenceStatus.COMPLETE;
        } else if (score >= 50) {
            status = ScrapeFieldConfidenceStatus.PARTIAL;
        } else {
            status = ScrapeFieldConfidenceStatus.LOW_CONFIDENCE;
        }

        return ReraSearchSummaryDto.builder()
                .totalExpectedFields(TOTAL_EXPECTED)
                .foundFields((int) foundCount)
                .missingFields((int) (TOTAL_EXPECTED - foundCount))
                .highConfidenceFields((int) highConf)
                .lowConfidenceFields((int) lowConf)
                .confidenceScore(score)
                .status(status)
                .build();
    }

    // -------------------------------------------------------------------------
    // RERA number injection guard
    // -------------------------------------------------------------------------

    private void injectReraNumberIfMissing(Map<String, String> found,
                                            List<ScrapeFieldResultDto> fieldResults,
                                            List<ScrapeMissingFieldDto> missingFields,
                                            String reraNumber) {
        if (found.containsKey("reraNumber")) return;

        // Replace the NOT_FOUND entry with a 100-confidence found entry
        fieldResults.removeIf(f -> "reraNumber".equals(f.getFieldKey()));
        missingFields.removeIf(f -> "reraNumber".equals(f.getFieldKey()));

        found.put("reraNumber", reraNumber);
        fieldResults.add(ScrapeFieldResultDto.builder()
                .section(ScrapeFieldSection.PROJECT_BASIC)
                .fieldKey("reraNumber")
                .fieldLabel("RERA Registration Number")
                .found(true)
                .value(reraNumber)
                .sourceLabel("Search request (verified by RERA page)")
                .confidence(100)
                .build());
    }

    // -------------------------------------------------------------------------
    // Key-value lookup
    // -------------------------------------------------------------------------

    private String lookupFirst(Map<String, String> kv, List<String> synonyms) {
        if (kv == null) return null;
        for (String synonym : synonyms) {
            String value = kv.get(synonym);
            if (value != null && !value.isBlank()) return value.strip();
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Value normalizers (used in ScrapeFieldResultDto.value for display)
    // -------------------------------------------------------------------------

    private Object normalizeValue(String fieldKey, String rawValue) {
        return switch (fieldKey) {
            case "possessionDate"  -> { LocalDate d = parseDate(rawValue);
                                        yield d != null ? d : rawValue; }
            case "status"          -> { ProjectStatus s = normalizeStatus(rawValue);
                                        yield s != null ? s : rawValue; }
            case "propertyTypes"   -> { Set<PropertyType> t = normalizePropertyTypes(rawValue);
                                        yield t.isEmpty() ? rawValue : t; }
            case "latitude",
                 "longitude"       -> { Double d = parseDouble(rawValue);
                                        yield d != null ? d : rawValue; }
            case "priceMin",
                 "priceMax"        -> { Long l = parseLong(rawValue);
                                        yield l != null ? l : rawValue; }
            case "totalLandAreaSqm" -> { Double d = parseAreaToSqm(rawValue);
                                         yield d != null ? d : rawValue; }
            default                -> rawValue;
        };
    }

    // -------------------------------------------------------------------------
    // Type parsers
    // -------------------------------------------------------------------------

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.strip();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        log.debug("Could not parse date: '{}'", raw);
        return null;
    }

    private ProjectStatus normalizeStatus(String raw) {
        if (raw == null) return null;
        String lower = raw.toLowerCase();
        if (lower.contains("ongoing") || lower.contains("under construction")
                || lower.contains("underconstruction") || lower.contains("in progress")) {
            return ProjectStatus.UNDER_CONSTRUCTION;
        }
        if (lower.contains("completed") || lower.contains("complete")) {
            return ProjectStatus.COMPLETED;
        }
        if (lower.contains("ready to move") || lower.contains("readytomove")) {
            return ProjectStatus.READY_TO_MOVE;
        }
        if (lower.contains("upcoming") || lower.contains("proposed")
                || lower.contains("approved") || lower.contains("new")) {
            return ProjectStatus.UPCOMING;
        }
        return null;
    }

    private Set<PropertyType> normalizePropertyTypes(String raw) {
        Set<PropertyType> types = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) return types;
        String lower = raw.toLowerCase(java.util.Locale.ROOT);

        // --- Residential sub-types (specific before generic) ---
        if (lower.contains("serviced apartment")) {
            types.add(PropertyType.SERVICED_APARTMENT);
            types.add(PropertyType.APARTMENT);
        }
        if (!types.contains(PropertyType.APARTMENT)
                && (lower.contains("apartment") || lower.contains("flat") || lower.contains("group housing"))) {
            types.add(PropertyType.APARTMENT);
        }
        if (lower.contains("studio")) {
            types.add(PropertyType.STUDIO);
        }
        if (lower.contains("villa")) {
            types.add(PropertyType.VILLA);
        }
        if (lower.contains("row house") || lower.contains("rowhouse")) {
            types.add(PropertyType.ROW_HOUSE);
        }
        if (lower.contains("penthouse")) {
            types.add(PropertyType.PENTHOUSE);
        }
        if (lower.contains("duplex")) {
            types.add(PropertyType.DUPLEX);
        }
        if (lower.contains("farmhouse") || lower.contains("farm house")) {
            types.add(PropertyType.FARMHOUSE);
        }
        if (lower.contains("builder floor")) {
            types.add(PropertyType.BUILDER_FLOOR);
        }
        if (lower.contains("independent house") || lower.contains("independent home")) {
            types.add(PropertyType.INDEPENDENT_HOUSE);
        }
        // RESIDENTIAL is the broad umbrella — add when raw mentions "residential" generically
        if (lower.contains("residential")) {
            types.add(PropertyType.RESIDENTIAL);
        }

        // --- Land / Plot (specific before generic) ---
        if (lower.contains("residential plot")) {
            types.add(PropertyType.RESIDENTIAL_PLOT);
            types.add(PropertyType.PLOT);
        }
        if (lower.contains("commercial plot")) {
            types.add(PropertyType.COMMERCIAL_PLOT);
            types.add(PropertyType.PLOT);
        }
        if (lower.contains("agricultural") || lower.contains("agriculture")) {
            types.add(PropertyType.AGRICULTURAL_LAND);
        }
        if (!types.contains(PropertyType.PLOT)
                && (lower.contains("plot") || lower.contains("plotted"))) {
            types.add(PropertyType.PLOT);
        }

        // --- Commercial sub-types (specific before generic) ---
        if (lower.contains("co-working") || lower.contains("coworking")) {
            types.add(PropertyType.CO_WORKING_SPACE);
        }
        if (lower.contains("food court")) {
            types.add(PropertyType.FOOD_COURT);
        }
        if (lower.contains("showroom")) {
            types.add(PropertyType.SHOWROOM);
        }
        if (lower.contains("retail") || lower.contains("shop")) {
            types.add(PropertyType.RETAIL_SHOP);
        }
        if (lower.contains("office")) {
            types.add(PropertyType.OFFICE_SPACE);
        }
        if (lower.contains("commercial")) {
            types.add(PropertyType.COMMERCIAL);
        }

        // --- Mixed Use / Hospitality ---
        if (lower.contains("mixed use") || lower.contains("mixed-use")) {
            types.add(PropertyType.MIXED_USE);
        }
        if (lower.contains("hotel")) {
            types.add(PropertyType.HOTEL);
        }
        if (lower.contains("resort")) {
            types.add(PropertyType.RESORT);
        }

        // --- Industrial / Institutional ---
        if (lower.contains("warehouse")) {
            types.add(PropertyType.WAREHOUSE);
            types.add(PropertyType.INDUSTRIAL);
        }
        if (lower.contains("factory")) {
            types.add(PropertyType.FACTORY);
            types.add(PropertyType.INDUSTRIAL);
        }
        if (!types.contains(PropertyType.INDUSTRIAL) && lower.contains("industrial")) {
            types.add(PropertyType.INDUSTRIAL);
        }
        if (lower.contains("institutional") || lower.contains("school")
                || lower.contains("college") || lower.contains("hospital project")) {
            types.add(PropertyType.INSTITUTIONAL);
        }

        return types;
    }

    private Double parseAreaToSqm(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Double numeric = parseDouble(raw);
        if (numeric == null) return null;
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("acre")) {
            return roundTwoDecimals(numeric * ACRE_TO_SQM);
        }
        if (lower.contains("hectare") || lower.contains(" ha")) {
            return roundTwoDecimals(numeric * HECTARE_TO_SQM);
        }
        return roundTwoDecimals(numeric);
    }

    private Double roundTwoDecimals(Double value) {
        if (value == null) return null;
        return Math.round(value * 100.0) / 100.0;
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String cleaned = raw.strip().replaceAll("[^0-9.\\-]", "");
            return cleaned.isBlank() ? null : Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String cleaned = raw.strip().replaceAll("[^0-9\\-]", "");
            return cleaned.isBlank() ? null : Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
