package com.brandPitara.sfs.buildercredibility.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in the exact field set every public Builder Credibility DTO exposes.
 * These DTOs already carry a real, computed credibilityScore/credibilityLabel
 * (an existing part of the shipped contract - not something this test
 * invents), so the point here is narrower: catch an accidental future
 * addition of an internal/moderation/private/UI-only field that was never
 * part of the reviewed public contract, without redesigning what's already
 * there. See GAP-030 / acceptance-criteria.md item 74 for why an
 * unbacked "verified"-style field is specifically disallowed on the
 * indicator/risk/metric rows.
 */
class BuilderCredibilityDtoFieldSafetyTest {

    @Test
    void builderCredibilityResponseExposesOnlyApprovedPublicFields() {
        assertThat(fieldNames(BuilderCredibilityResponse.class)).containsExactlyInAnyOrder(
            "builderId", "builderName", "builderLogoUrl", "cityName",
            "credibilityScore", "credibilityLabel", "summary", "confidenceLabel",
            "trackedProjectsCount",
            "metrics", "scoreBreakdown", "recentProjectEvidence",
            "positiveIndicators", "observedRisks"
        );
    }

    @Test
    void builderCredibilityCardResponseExposesOnlyApprovedPublicFields() {
        assertThat(fieldNames(BuilderCredibilityCardResponse.class)).containsExactlyInAnyOrder(
            "builderId", "builderName", "builderLogoUrl", "cityName",
            "credibilityScore", "credibilityLabel",
            "projectsTracked", "onTrackRecord", "promisesMet", "complianceStrength",
            "projectsTrackedCount", "onTrackRecordPercent", "promisesMetPercent", "complianceStrengthPercent",
            "summary", "confidenceLabel",
            "highlightsAvailable", "highlightCtaLabel"
        );
    }

    @Test
    void builderCredibilitySummaryResponseExposesOnlyApprovedPublicFields() {
        assertThat(fieldNames(BuilderCredibilitySummaryResponse.class)).containsExactlyInAnyOrder(
            "builderId", "builderName", "builderLogoUrl", "cityName",
            "credibilityScore", "credibilityLabel",
            "projectsTrackedCount", "onTrackRecordPercent", "promisesMetPercent", "complianceStrengthPercent",
            "summary", "confidenceLabel"
        );
    }

    @Test
    void indicatorDtoHasNoUnbackedVerifiedOrBadgeField() {
        // Mobile renders an unconditional "Verified" label next to indicator
        // rows with no backing field (RISK-024) - this test guards the
        // backend contract side of that finding: the indicator DTO must never
        // grow a field that would retroactively "justify" that label without
        // a real, reviewed verification concept behind it.
        assertThat(fieldNames(BuilderCredibilityIndicatorDto.class))
            .containsExactlyInAnyOrder("title", "description");
    }

    @Test
    void riskDtoExposesOnlyTitleAndDescription() {
        assertThat(fieldNames(BuilderCredibilityRiskDto.class))
            .containsExactlyInAnyOrder("title", "description");
    }

    @Test
    void metricDtoExposesOnlyKeyLabelValueSubtitle() {
        assertThat(fieldNames(BuilderCredibilityMetricDto.class))
            .containsExactlyInAnyOrder("key", "label", "value", "subtitle");
    }

    @Test
    void scoreBreakdownDtoExposesOnlyKeyLabelScoreMaxScoreSummary() {
        assertThat(fieldNames(BuilderCredibilityScoreBreakdownDto.class))
            .containsExactlyInAnyOrder("key", "label", "score", "maxScore", "summary");
    }

    @Test
    void projectEvidenceDtoExposesOnlyApprovedPublicFields() {
        // "verified" here IS a real, backend-computed field
        // (projectVerificationPercent >= 70 in BuilderCredibilityServiceImpl),
        // unlike the indicator-row label mobile renders - it belongs on this
        // DTO precisely because it has real evidence behind it.
        assertThat(fieldNames(BuilderCredibilityProjectEvidenceDto.class)).containsExactlyInAnyOrder(
            "projectId", "projectName", "projectSlug", "cityName",
            "constructionProgressPercent", "timelineStatus", "delayDays",
            "promiseFulfilmentPercent", "complianceStrengthPercent", "verified"
        );
    }

    @Test
    void noCredibilityDtoContainsAnInternalOrModerationOrContactField() {
        Set<String> disallowedSubstrings = Set.of(
            "internal", "moderation", "note", "draft", "remark",
            "phone", "email", "whatsapp", "contact", "ipaddress", "userid", "dashboarduser"
        );

        Class<?>[] publicDtos = {
            BuilderCredibilityResponse.class,
            BuilderCredibilityCardResponse.class,
            BuilderCredibilitySummaryResponse.class,
            BuilderCredibilityIndicatorDto.class,
            BuilderCredibilityRiskDto.class,
            BuilderCredibilityMetricDto.class,
            BuilderCredibilityScoreBreakdownDto.class,
            BuilderCredibilityProjectEvidenceDto.class
        };

        for (Class<?> dto : publicDtos) {
            for (String fieldName : fieldNames(dto)) {
                String lower = fieldName.toLowerCase();
                assertThat(disallowedSubstrings.stream().noneMatch(lower::contains))
                    .as("field '%s' on %s looks like it may leak internal/contact data", fieldName, dto.getSimpleName())
                    .isTrue();
            }
        }
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getName)
            .filter(name -> !name.startsWith("$") && !name.equals("serialVersionUID"))
            .collect(Collectors.toSet());
    }
}
