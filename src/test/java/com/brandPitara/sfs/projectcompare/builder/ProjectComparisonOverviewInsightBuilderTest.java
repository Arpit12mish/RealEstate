package com.brandPitara.sfs.projectcompare.builder;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonOverviewInsightResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectComparisonOverviewInsightBuilderTest {

    private final ProjectComparisonOverviewInsightBuilder builder = new ProjectComparisonOverviewInsightBuilder();

    @Test
    void buildsDeterministicBlocksAndVerdictForTwoProjects() {
        ProjectEntity alpha = project(1L, "Alpha", 17_100_000L, 14_328L, LocalDate.of(2029, 1, 1));
        ProjectEntity beta = project(2L, "Beta", 16_800_000L, 10_758L, LocalDate.of(2025, 11, 1));

        ComparisonOverviewInsightResponse result = builder.build(
                List.of(alpha, beta),
                Map.of(
                        1L, snapshot(alpha, LocalDate.of(2029, 1, 1), 45, 120, 65),
                        2L, snapshot(beta, LocalDate.of(2025, 11, 1), 75, 10, 80)
                ),
                Map.of(),
                Map.of(
                        1L, List.of(floorPlan(alpha, "72")),
                        2L, List.of(floorPlan(beta, "78"))
                ),
                Map.of(
                        1L, masterPlan(alpha, "85", 850, "10"),
                        2L, masterPlan(beta, "63", 900, "10")
                ),
                Map.of()
        );

        assertThat(result.getTitle()).isEqualTo("Alpha vs Beta — Clear, Data-Led Comparison");
        assertThat(result.getBlocks()).extracting("key")
                .containsExactly("PRICING_VALUE", "POSSESSION_TIMELINE", "LIFESTYLE_LIVING", "CONSTRUCTION_CONFIDENCE");
        assertThat(result.getBlocks().get(0).getBody()).contains("₹1.68 Cr", "₹10,758/sq.ft.");
        assertThat(result.getBlocks().get(1).getBody()).contains("November 2025", "January 2029");
        assertThat(result.getBlocks().get(2).getBody()).contains("open space", "carpet efficiency");
        assertThat(result.getVerdict()).isNotNull();
        assertThat(result.getVerdict().getHeading()).isEqualTo("Final Verdict");
    }

    @Test
    void missingOptionalDataSkipsBlocksWithoutThrowing() {
        ProjectEntity alpha = project(1L, "Alpha", 10_000_000L, null, null);
        ProjectEntity beta = project(2L, "Beta", 12_000_000L, null, null);

        ComparisonOverviewInsightResponse result = builder.build(
                List.of(alpha, beta), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(result.getBlocks()).extracting("key").containsExactly("PRICING_VALUE");
        assertThat(result.getVerdict()).isNotNull();
    }

    @Test
    void tiedCategoriesProduceNeutralVerdict() {
        ProjectEntity alpha = project(1L, "Alpha", 10_000_000L, 10_000L, LocalDate.of(2027, 6, 1));
        ProjectEntity beta = project(2L, "Beta", 10_000_000L, 10_000L, LocalDate.of(2027, 6, 1));

        ComparisonOverviewInsightResponse result = builder.build(
                List.of(alpha, beta), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(result.getVerdict().getWinnerProjectId()).isNull();
        assertThat(result.getVerdict().getTone()).isEqualTo("NEUTRAL");
        assertThat(result.getVerdict().getBody()).contains("Both projects serve different buyer priorities");
    }

    @Test
    void supportsThreeProjectsWithRankingLanguage() {
        ProjectEntity alpha = project(1L, "Alpha", 20_000_000L, 15_000L, LocalDate.of(2028, 1, 1));
        ProjectEntity beta = project(2L, "Beta", 18_000_000L, 11_000L, LocalDate.of(2026, 1, 1));
        ProjectEntity gamma = project(3L, "Gamma", 19_000_000L, 13_000L, LocalDate.of(2027, 1, 1));

        ComparisonOverviewInsightResponse result = builder.build(
                List.of(alpha, beta, gamma), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(result.getBlocks().get(0).getBody()).contains("Among the compared projects", "Beta");
        assertThat(result.getVerdict()).isNotNull();
    }

    private ProjectEntity project(Long id, String name, Long minPrice, Long pricePerSqft, LocalDate possession) {
        ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setName(name);
        project.setPriceMin(minPrice);
        project.setAveragePricePerSqft(pricePerSqft);
        project.setPossessionDate(possession);
        return project;
    }

    private ProjectMeterSnapshotEntity snapshot(
            ProjectEntity project, LocalDate possession, int progress, int delay, int amenity
    ) {
        return ProjectMeterSnapshotEntity.builder()
                .project(project)
                .latestReraCompletionDate(possession)
                .constructionProgressPercent(progress)
                .delayDays(delay)
                .amenityScore(amenity)
                .build();
    }

    private ProjectFloorPlanEntity floorPlan(ProjectEntity project, String efficiency) {
        return ProjectFloorPlanEntity.builder()
                .project(project)
                .carpetEfficiencyPercent(new BigDecimal(efficiency))
                .build();
    }

    private ProjectMasterPlanEntity masterPlan(
            ProjectEntity project, String openSpace, int units, String acres
    ) {
        return ProjectMasterPlanEntity.builder()
                .project(project)
                .openSpacePercent(new BigDecimal(openSpace))
                .totalUnits(units)
                .totalLandAreaValue(new BigDecimal(acres))
                .totalLandAreaUnit(MasterPlanAreaUnit.ACRE)
                .build();
    }
}
