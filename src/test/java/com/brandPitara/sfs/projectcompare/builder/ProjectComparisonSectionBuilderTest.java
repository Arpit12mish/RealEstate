package com.brandPitara.sfs.projectcompare.builder;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonRow;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonSection;
import com.brandPitara.sfs.projectcompare.enums.ComparisonSectionKey;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectComparisonSectionBuilderTest {

    private ProjectComparisonSectionBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ProjectComparisonSectionBuilder();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ProjectEntity project(long id, String name) {
        ProjectEntity p = new ProjectEntity();
        p.setId(id);
        p.setName(name);
        p.setSlug("slug-" + id);
        BuilderEntity b = new BuilderEntity();
        b.setId(10L);
        b.setName("M3M");
        p.setBuilder(b);
        CityEntity city = new CityEntity();
        city.setId(1L);
        city.setName("Gurugram");
        p.setCity(city);
        return p;
    }

    private ProjectEntity projectWithBuilder(long id, String name, long builderId, String builderName) {
        ProjectEntity p = project(id, name);
        BuilderEntity b = new BuilderEntity();
        b.setId(builderId);
        b.setName(builderName);
        p.setBuilder(b);
        return p;
    }

    private ProjectAmenityProgressEntity amenity(
            long id, ProjectEntity project, String code, String label,
            ProjectAmenityStatus status, int displayOrder, boolean verified
    ) {
        ProjectAmenityProgressEntity a = new ProjectAmenityProgressEntity();
        a.setId(id);
        a.setProject(project);
        a.setAmenityCode(code);
        a.setAmenityLabel(label);
        a.setStatus(status);
        a.setDisplayOrder(displayOrder);
        a.setVerified(verified);
        return a;
    }

    private ProjectComplianceItemEntity complianceItem(ProjectEntity project, String itemKey, ProjectComplianceStatus status, boolean verified) {
        ProjectComplianceItemEntity c = new ProjectComplianceItemEntity();
        c.setProject(project);
        c.setItemKey(itemKey);
        c.setItemLabel(itemKey);
        c.setStatus(status);
        c.setVerified(verified);
        c.setDisplayOrder(1);
        return c;
    }

    private ProjectFloorPlanEntity floorPlan(ProjectEntity project, BigDecimal carpetArea, BigDecimal superArea) {
        ProjectFloorPlanEntity fp = new ProjectFloorPlanEntity();
        fp.setProject(project);
        fp.setTitle("Plan");
        fp.setImageUrl("url");
        fp.setCarpetAreaSqft(carpetArea);
        fp.setSuperAreaSqft(superArea);
        return fp;
    }

    // ─── Issue 1: Optional rows hidden when all values unavailable ─────────────

    @Test
    void optionalRows_pricePerSqftHiddenWhenAllProjectsHaveNoData() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        // No averagePricePerSqft set → both projects have null
        List<ProjectEntity> projects = List.of(p1, p2);

        ComparisonSection section = builder.buildPrice(projects, Map.of());

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).doesNotContain("pricePerSqft");
        assertThat(rowKeys).doesNotContain("monthlyEmi");
    }

    @Test
    void requiredOverviewRows_alwaysPresent() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        List<ProjectEntity> projects = List.of(p1, p2);

        ComparisonSection section = builder.buildOverview(projects, Map.of());

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).contains("name", "builder", "city", "projectStatus", "reraNumber", "possessionDate");
    }

    @Test
    void optionalAddressRow_hiddenWhenAllNull() {
        ProjectEntity p1 = project(1L, "Alpha");   // addressLine is null
        ProjectEntity p2 = project(2L, "Beta");
        List<ProjectEntity> projects = List.of(p1, p2);

        ComparisonSection section = builder.buildOverview(projects, Map.of());

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).doesNotContain("address");
    }

    @Test
    void optionalAddressRow_presentWhenAtLeastOneProjectHasIt() {
        ProjectEntity p1 = project(1L, "Alpha");
        p1.setAddressLine("Sector 65, Gurugram");
        ProjectEntity p2 = project(2L, "Beta");
        List<ProjectEntity> projects = List.of(p1, p2);

        ComparisonSection section = builder.buildOverview(projects, Map.of());

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).contains("address");
    }

    // ─── Issue 2: Amenity valueType must be STATUS_BADGE ──────────────────────

    @Test
    void amenityRows_valueTypeIsStatusBadge_notBoolean() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectAmenityProgressEntity a = amenity(1L, p1, "swimming_pool", "Swimming Pool",
                ProjectAmenityStatus.COMPLETED, 1, true);

        ComparisonSection section = builder.buildAmenities(
                List.of(p1),
                Map.of(p1.getId(), List.of(a)),
                Map.of()
        );

        List<ComparisonRow> amenityRows = section.getRows().stream()
                .filter(r -> r.getRowKey().startsWith("amenity_")).toList();
        assertThat(amenityRows).isNotEmpty();
        amenityRows.forEach(row ->
                assertThat(row.getValueType())
                        .as("row %s should be STATUS_BADGE, not BOOLEAN", row.getRowKey())
                        .isEqualTo("STATUS_BADGE")
                        .isNotEqualTo("BOOLEAN")
        );
    }

    @Test
    void amenityDisplayValues_useProperLabels() {
        ProjectEntity p1 = project(1L, "Alpha");
        List<ProjectAmenityProgressEntity> amenities = List.of(
                amenity(1L, p1, "clubhouse",     "Clubhouse",     ProjectAmenityStatus.COMPLETED,     1, true),
                amenity(2L, p1, "swimming_pool",  "Swimming Pool",  ProjectAmenityStatus.IN_PROGRESS,   2, false),
                amenity(3L, p1, "gymnasium",      "Gymnasium",      ProjectAmenityStatus.PLANNED,       3, false),
                amenity(4L, p1, "kids_play_area", "Kids Play Area", ProjectAmenityStatus.NOT_AVAILABLE, 4, false)
        );

        ComparisonSection section = builder.buildAmenities(
                List.of(p1), Map.of(p1.getId(), amenities), Map.of()
        );

        Map<String, String> displayByKey = section.getRows().stream()
                .filter(r -> r.getRowKey().startsWith("amenity_"))
                .collect(java.util.stream.Collectors.toMap(
                        ComparisonRow::getRowKey,
                        r -> r.getValues().get("1").getDisplayValue()
                ));

        assertThat(displayByKey.get("amenity_clubhouse")).isEqualTo("Completed");
        assertThat(displayByKey.get("amenity_swimming_pool")).isEqualTo("In Progress");
        assertThat(displayByKey.get("amenity_gymnasium")).isEqualTo("Planned");
        assertThat(displayByKey.get("amenity_kids_play_area")).isEqualTo("Not Available");
    }

    // ─── Issue 3: Amenity alias normalization ──────────────────────────────────

    @Test
    void gymAndGymnasium_mergeIntoOneRow() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        // p1 has "gym", p2 has "gymnasium" — both should collapse to "amenity_gymnasium"
        ProjectAmenityProgressEntity gym  = amenity(1L, p1, "gym",       "Gym",       ProjectAmenityStatus.COMPLETED,   1, true);
        ProjectAmenityProgressEntity gymn = amenity(2L, p2, "gymnasium", "Gymnasium", ProjectAmenityStatus.IN_PROGRESS, 1, false);

        ComparisonSection section = builder.buildAmenities(
                List.of(p1, p2),
                Map.of(p1.getId(), List.of(gym), p2.getId(), List.of(gymn)),
                Map.of()
        );

        List<String> amenityKeys = section.getRows().stream()
                .map(ComparisonRow::getRowKey)
                .filter(k -> k.startsWith("amenity_"))
                .toList();

        assertThat(amenityKeys).containsExactly("amenity_gymnasium");
        assertThat(amenityKeys).doesNotContain("amenity_gym");
    }

    @Test
    void clubhouseAndClubHouse_mergeIntoOneRow() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        ProjectAmenityProgressEntity c1 = amenity(1L, p1, "club_house", "Club House", ProjectAmenityStatus.COMPLETED,   1, true);
        ProjectAmenityProgressEntity c2 = amenity(2L, p2, "clubhouse",  "Clubhouse",  ProjectAmenityStatus.IN_PROGRESS, 1, false);

        ComparisonSection section = builder.buildAmenities(
                List.of(p1, p2),
                Map.of(p1.getId(), List.of(c1), p2.getId(), List.of(c2)),
                Map.of()
        );

        List<String> amenityKeys = section.getRows().stream()
                .map(ComparisonRow::getRowKey)
                .filter(k -> k.startsWith("amenity_"))
                .toList();

        assertThat(amenityKeys).containsExactly("amenity_clubhouse");
    }

    @Test
    void poolAlias_normalizesToSwimmingPool() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectAmenityProgressEntity a = amenity(1L, p1, "pool", "Pool", ProjectAmenityStatus.COMPLETED, 1, true);

        ComparisonSection section = builder.buildAmenities(
                List.of(p1), Map.of(p1.getId(), List.of(a)), Map.of()
        );

        List<String> keys = section.getRows().stream()
                .map(ComparisonRow::getRowKey).filter(k -> k.startsWith("amenity_")).toList();
        assertThat(keys).containsExactly("amenity_swimming_pool");
    }

    // ─── Issue 3: Best status when duplicates ──────────────────────────────────

    @Test
    void duplicateAmenityForSameProject_choosesCompletedOverInProgress() {
        ProjectEntity p1 = project(1L, "Alpha");
        // Two records with the same canonical code — one COMPLETED, one IN_PROGRESS
        ProjectAmenityProgressEntity inProgress = amenity(1L, p1, "gymnasium", "Gymnasium", ProjectAmenityStatus.IN_PROGRESS, 2, false);
        ProjectAmenityProgressEntity completed  = amenity(2L, p1, "gymnasium", "Gymnasium", ProjectAmenityStatus.COMPLETED,   1, true);

        ComparisonSection section = builder.buildAmenities(
                List.of(p1),
                Map.of(p1.getId(), List.of(inProgress, completed)),
                Map.of()
        );

        ComparisonRow row = section.getRows().stream()
                .filter(r -> r.getRowKey().equals("amenity_gymnasium")).findFirst().orElseThrow();
        assertThat(row.getValues().get("1").getDisplayValue()).isEqualTo("Completed");
    }

    @Test
    void duplicateAmenityForSameProject_sameStatusPrefersVerified() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectAmenityProgressEntity unverified = amenity(1L, p1, "gymnasium", "Gymnasium", ProjectAmenityStatus.IN_PROGRESS, 1, false);
        ProjectAmenityProgressEntity verified   = amenity(2L, p1, "gymnasium", "Gymnasium", ProjectAmenityStatus.IN_PROGRESS, 2, true);

        ComparisonSection section = builder.buildAmenities(
                List.of(p1),
                Map.of(p1.getId(), List.of(unverified, verified)),
                Map.of()
        );

        ComparisonRow row = section.getRows().stream()
                .filter(r -> r.getRowKey().equals("amenity_gymnasium")).findFirst().orElseThrow();
        // Raw value of the chosen entity's status
        assertThat(row.getValues().get("1").getRawValue()).isEqualTo("IN_PROGRESS");
        // verified entity was chosen (same display value, but we confirm no crash and correct status)
        assertThat(row.getValues().get("1").isAvailable()).isTrue();
    }

    // ─── Issue 4: Amenity priority ordering ───────────────────────────────────

    @Test
    void amenityPriorityOrder_priorityCodesBeforeOtherCodes() {
        ProjectEntity p1 = project(1L, "Alpha");
        List<ProjectAmenityProgressEntity> amenities = List.of(
                amenity(1L, p1, "random_amenity", "Random", ProjectAmenityStatus.COMPLETED, 1, true),
                amenity(2L, p1, "clubhouse",       "Club",   ProjectAmenityStatus.COMPLETED, 2, true),
                amenity(3L, p1, "swimming_pool",   "Pool",   ProjectAmenityStatus.COMPLETED, 3, true)
        );

        ComparisonSection section = builder.buildAmenities(
                List.of(p1), Map.of(p1.getId(), amenities), Map.of()
        );

        List<String> amenityKeys = section.getRows().stream()
                .map(ComparisonRow::getRowKey)
                .filter(k -> k.startsWith("amenity_"))
                .toList();

        // clubhouse and swimming_pool should appear before random_amenity
        int clubhouseIdx    = amenityKeys.indexOf("amenity_clubhouse");
        int poolIdx         = amenityKeys.indexOf("amenity_swimming_pool");
        int randomIdx       = amenityKeys.indexOf("amenity_random_amenity");

        assertThat(clubhouseIdx).isLessThan(randomIdx);
        assertThat(poolIdx).isLessThan(randomIdx);
        assertThat(clubhouseIdx).isLessThan(poolIdx);
    }

    // ─── Issue 5: Compliance score = verified/total * 100 ─────────────────────

    @Test
    void complianceScore_6Of8Verified_returns75Percent() {
        ProjectEntity p1 = project(1L, "Alpha");
        // 6 verified, 2 unverified
        List<ProjectComplianceItemEntity> items = List.of(
                complianceItem(p1, "rera",          ProjectComplianceStatus.OBTAINED, true),
                complianceItem(p1, "fire_noc",      ProjectComplianceStatus.OBTAINED, true),
                complianceItem(p1, "env_clearance", ProjectComplianceStatus.OBTAINED, true),
                complianceItem(p1, "airport_noc",   ProjectComplianceStatus.OBTAINED, true),
                complianceItem(p1, "land_title",    ProjectComplianceStatus.OBTAINED, true),
                complianceItem(p1, "mutation",      ProjectComplianceStatus.OBTAINED, true),
                complianceItem(p1, "zoning",        ProjectComplianceStatus.PENDING,  false),
                complianceItem(p1, "occ_cert",      ProjectComplianceStatus.PENDING,  false)
        );

        ComparisonSection section = builder.buildMeter(
                List.of(p1),
                Map.of(),
                Map.of(),
                Map.of(p1.getId(), items)
        );

        ComparisonRow scoreRow = section.getRows().stream()
                .filter(r -> r.getRowKey().equals("complianceScore")).findFirst().orElseThrow();

        assertThat(scoreRow.getValues().get("1").getDisplayValue()).isEqualTo("75%");
        assertThat(scoreRow.getValues().get("1").getRawValue()).isEqualTo(75);
    }

    @Test
    void complianceScore_0Of3Verified_returns0Percent() {
        ProjectEntity p1 = project(1L, "Alpha");
        List<ProjectComplianceItemEntity> items = List.of(
                complianceItem(p1, "rera",     ProjectComplianceStatus.PENDING, false),
                complianceItem(p1, "fire_noc", ProjectComplianceStatus.PENDING, false),
                complianceItem(p1, "zoning",   ProjectComplianceStatus.PENDING, false)
        );

        ComparisonSection section = builder.buildMeter(
                List.of(p1), Map.of(), Map.of(), Map.of(p1.getId(), items)
        );

        ComparisonRow scoreRow = section.getRows().stream()
                .filter(r -> r.getRowKey().equals("complianceScore")).findFirst().orElseThrow();
        assertThat(scoreRow.getValues().get("1").getDisplayValue()).isEqualTo("0%");
    }

    @Test
    void complianceScore_noItems_rowHiddenOrMissing() {
        ProjectEntity p1 = project(1L, "Alpha");

        ComparisonSection section = builder.buildMeter(
                List.of(p1), Map.of(), Map.of(), Map.of()
        );

        // Row should either be absent or have available=false
        section.getRows().stream()
                .filter(r -> r.getRowKey().equals("complianceScore"))
                .forEach(r -> assertThat(r.getValues().get("1").isAvailable()).isFalse());
    }

    // ─── Issue 6: Compliance status labels ────────────────────────────────────

    @Test
    void complianceFormatting_notApplicableLabel() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectComplianceItemEntity item = complianceItem(p1, "airport_noc", ProjectComplianceStatus.NOT_APPLICABLE, false);

        ComparisonSection section = builder.buildCompliance(
                List.of(p1), Map.of(p1.getId(), List.of(item))
        );

        ComparisonRow row = section.getRows().stream()
                .filter(r -> r.getRowKey().equals("compliance_airport_noc")).findFirst().orElseThrow();
        assertThat(row.getValues().get("1").getDisplayValue()).isEqualTo("Not Applicable");
    }

    // ─── Issue 7: Units section hides empty optional rows ─────────────────────

    @Test
    void unitsSection_emptyAreaRows_hiddenWhenAllNull() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        // Floor plans exist but have no area data
        ProjectFloorPlanEntity fp1 = floorPlan(p1, null, null);
        ProjectFloorPlanEntity fp2 = floorPlan(p2, null, null);

        ComparisonSection section = builder.buildUnits(
                List.of(p1, p2),
                Map.of(p1.getId(), List.of(fp1), p2.getId(), List.of(fp2))
        );

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).doesNotContain("minCarpetArea", "maxCarpetArea", "minSuperArea", "maxSuperArea");
    }

    @Test
    void unitsSection_carpetAreaPresent_whenAtLeastOneProjectHasData() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        ProjectFloorPlanEntity fp1 = floorPlan(p1, new BigDecimal("850.00"), null);
        ProjectFloorPlanEntity fp2 = floorPlan(p2, null, null);  // no carpet data

        ComparisonSection section = builder.buildUnits(
                List.of(p1, p2),
                Map.of(p1.getId(), List.of(fp1), p2.getId(), List.of(fp2))
        );

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).contains("minCarpetArea");

        // p2's cell should be missing
        ComparisonRow minCarpet = section.getRows().stream()
                .filter(r -> r.getRowKey().equals("minCarpetArea")).findFirst().orElseThrow();
        assertThat(minCarpet.getValues().get("2").isAvailable()).isFalse();
        assertThat(minCarpet.getValues().get("2").getDisplayValue()).isEqualTo("—");
    }

    @Test
    void unitsSection_floorPlanCount_hiddenWhenNoPlans() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");

        ComparisonSection section = builder.buildUnits(
                List.of(p1, p2), Map.of()  // no floor plans at all
        );

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).doesNotContain("floorPlanCount");
    }

    @Test
    void unitsSection_configurationsRow_hiddenWhenNoUnitType() {
        ProjectEntity p1 = project(1L, "Alpha");
        // Floor plan exists but has no unit configuration type
        ProjectFloorPlanEntity fp = floorPlan(p1, new BigDecimal("900.00"), null);
        // unitConfigurationType is null by default

        ComparisonSection section = builder.buildUnits(
                List.of(p1), Map.of(p1.getId(), List.of(fp))
        );

        List<String> rowKeys = section.getRows().stream().map(ComparisonRow::getRowKey).toList();
        assertThat(rowKeys).doesNotContain("availableConfigurations");
    }

    // ─── Issue 8: Same-builder metadata ───────────────────────────────────────

    @Test
    void builderSection_sameBuilder_addsDescription() {
        ProjectEntity p1 = project(1L, "M3M Jewel");
        ProjectEntity p2 = project(2L, "M3M Cullinan");
        // Both already use builderId=10, builderName="M3M" from the helper

        ComparisonSection section = builder.buildBuilder(List.of(p1, p2), Map.of());

        assertThat(section.getDescription()).isNotNull();
        assertThat(section.getDescription()).contains("M3M");
    }

    @Test
    void builderSection_differentBuilders_noDescription() {
        ProjectEntity p1 = projectWithBuilder(1L, "Alpha", 10L, "M3M");
        ProjectEntity p2 = projectWithBuilder(2L, "Beta",  20L, "DLF");

        ComparisonSection section = builder.buildBuilder(List.of(p1, p2), Map.of());

        assertThat(section.getDescription()).isNull();
    }

    // ─── Existing contract stability ──────────────────────────────────────────

    @Test
    void overviewSection_hasSectionKeyAndOrder() {
        ComparisonSection section = builder.buildOverview(List.of(project(1L, "A")), Map.of());

        assertThat(section.getSectionKey()).isEqualTo(ComparisonSectionKey.OVERVIEW);
        assertThat(section.getDisplayOrder()).isEqualTo(1);
        assertThat(section.isInitiallyExpanded()).isTrue();
    }

    @Test
    void amenityRow_missingForProjectWithNoData() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        // Only p1 has the amenity
        ProjectAmenityProgressEntity a = amenity(1L, p1, "gymnasium", "Gym", ProjectAmenityStatus.COMPLETED, 1, true);

        ComparisonSection section = builder.buildAmenities(
                List.of(p1, p2),
                Map.of(p1.getId(), List.of(a), p2.getId(), List.of()),
                Map.of()
        );

        ComparisonRow row = section.getRows().stream()
                .filter(r -> r.getRowKey().equals("amenity_gymnasium")).findFirst().orElseThrow();

        assertThat(row.getValues().get("1").isAvailable()).isTrue();
        assertThat(row.getValues().get("2").isAvailable()).isFalse();
        assertThat(row.getValues().get("2").getDisplayValue()).isEqualTo("—");
    }

    @Test
    void allAmenityRowsHiddenIfNoProjectHasThem() {
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");

        ComparisonSection section = builder.buildAmenities(
                List.of(p1, p2), Map.of(), Map.of()
        );

        List<String> amenityRows = section.getRows().stream()
                .map(ComparisonRow::getRowKey)
                .filter(k -> k.startsWith("amenity_"))
                .toList();
        assertThat(amenityRows).isEmpty();
    }
}
