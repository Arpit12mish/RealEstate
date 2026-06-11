package com.brandPitara.sfs.builderimprovement.composer;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builderimprovement.dto.response.*;
import com.brandPitara.sfs.builderimprovement.entity.*;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementIssueStatus;
import com.brandPitara.sfs.builderimprovement.mapper.BuilderImprovementMapper;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class BuilderImprovementPageComposerImpl implements BuilderImprovementPageComposer {

    private final BuilderImprovementMapper mapper;

    @Override
    public BuilderImprovementResponse compose(
            BuilderImprovementProfileEntity profile,
            List<BuilderImprovementActionEntity> actions,
            List<BuilderImprovementIssueEntity> issues,
            List<BuilderAfterSalesUpgradeEntity> afterSalesUpgrades,
            List<BuilderImprovementTimelineEntity> timeline
    ) {
        BuilderEntity builder = profile.getBuilder();
        ProjectEntity project = profile.getProject();

        return BuilderImprovementResponse.builder()
                .profileId(profile.getId())

                .builderId(builder != null ? builder.getId() : null)
                .builderName(builder != null ? builder.getName() : null)
                .builderLogoUrl(builder != null ? builder.getLogoUrl() : null)

                .projectId(project != null ? project.getId() : null)
                .projectName(project != null ? project.getName() : null)
                .projectSlug(project != null ? project.getSlug() : null)

                .badgeText(profile.getBadgeText())
                .heroTitle(profile.getHeroTitle())
                .heroSubtitle(profile.getHeroSubtitle())
                .heroImageUrl(profile.getHeroImageUrl())
                .verifiedByText(profile.getVerifiedByText())
                .summary(profile.getSummary())

                .overallStatus(profile.getOverallStatus() != null ? profile.getOverallStatus().name() : null)
                .evidenceStatus(profile.getEvidenceStatus() != null ? profile.getEvidenceStatus().name() : null)
                .lastReviewedAt(profile.getLastReviewedAt())

                .builderResponse(mapper.toBuilderResponse(profile))

                .actions(actions.stream()
                        .map(mapper::toActionResponse)
                        .toList())

                .issueClosureMomentum(buildIssueMomentum(profile, issues))

                .afterSalesUpgrades(afterSalesUpgrades.stream()
                        .map(mapper::toAfterSalesUpgradeResponse)
                        .toList())

                .timeline(timeline.stream()
                        .map(mapper::toTimelineResponse)
                        .toList())

                .build();
    }

    private BuilderImprovementIssueMomentumResponse buildIssueMomentum(
            BuilderImprovementProfileEntity profile,
            List<BuilderImprovementIssueEntity> issues
    ) {
        List<BuilderImprovementIssueEntity> trackableIssues = issues.stream()
                .filter(issue -> issue.getStatus() != BuilderImprovementIssueStatus.CANCELLED)
                .sorted(Comparator
                        .comparing(BuilderImprovementIssueEntity::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BuilderImprovementIssueEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        long totalProblems = trackableIssues.size();

        long resolved = trackableIssues.stream()
                .filter(issue -> issue.getStatus() == BuilderImprovementIssueStatus.RESOLVED)
                .count();

        long problemsLeft = trackableIssues.stream()
                .filter(issue -> issue.getStatus() == BuilderImprovementIssueStatus.PENDING
                        || issue.getStatus() == BuilderImprovementIssueStatus.IN_PROGRESS
                        || issue.getStatus() == BuilderImprovementIssueStatus.REOPENED)
                .count();

        int resolutionPercent = totalProblems == 0
                ? 0
                : (int) Math.floor((resolved * 100.0) / totalProblems);

        String calculationText = totalProblems == 0
                ? "No tracked issues in the selected period."
                : "Calculation: (" + resolved + " Resolved / " + totalProblems + " Total) × 100 = " + resolutionPercent + "%.";

        Integer averageResolutionDays = calculateAverageResolutionDays(trackableIssues);

        List<BuilderImprovementIssueResponse> resolvedIssues = trackableIssues.stream()
                .filter(issue -> issue.getStatus() == BuilderImprovementIssueStatus.RESOLVED)
                .sorted(Comparator
                        .comparing(BuilderImprovementIssueEntity::getResolvedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BuilderImprovementIssueEntity::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(mapper::toIssueResponse)
                .toList();

        List<BuilderImprovementIssueResponse> inProgressIssues = trackableIssues.stream()
                .filter(issue -> issue.getStatus() == BuilderImprovementIssueStatus.PENDING
                        || issue.getStatus() == BuilderImprovementIssueStatus.IN_PROGRESS
                        || issue.getStatus() == BuilderImprovementIssueStatus.REOPENED)
                .sorted(Comparator
                        .comparing(BuilderImprovementIssueEntity::getTargetResolutionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BuilderImprovementIssueEntity::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(mapper::toIssueResponse)
                .toList();

        List<BuilderImprovementIssueResponse> issueHistory = trackableIssues.stream()
                .map(mapper::toIssueResponse)
                .toList();

        return BuilderImprovementIssueMomentumResponse.builder()
                .title("Issue Closure Momentum")
                .subtitle("Verified complaint resolution progress")
                .infoText("\"Issue Closure Momentum\" refers to the rate and efficiency at which construction defects, site snags, or project management challenges are resolved and officially signed off as complete.")

                .detailTitle("Issue Closure Momentum")
                .detailSubtitle("Continuous performance tracking of project complaints and their resolution lifecycle.")

                .periodLabel("6MO")
                .totalCardLabel("TOTAL (6MO)")
                .problemsBefore(totalProblems)

                .averageTimeLabel("AVG. TIME")
                .averageResolutionDays(averageResolutionDays)
                .averageResolutionDaysLabel(averageResolutionDays == null ? null : averageResolutionDays + "d")

                .problemsLeft(problemsLeft)
                .issuesResolved(resolved)
                .resolutionPercent(resolutionPercent)
                .resolutionTrendLabel(resolutionPercent + "% Improvement")
                .calculationText(calculationText)

                .trend(buildTrend(trackableIssues))

                .resolvedTabLabel("Resolved")
                .inProgressTabLabel("In Progress")
                .resolvedIssues(resolvedIssues)
                .inProgressIssues(inProgressIssues)

                .issueHistorySheetTitle("Issue History")
                .issueHistory(issueHistory)

                .footerText(resolveFooterText(profile))
                .lastUpdatedAt(resolveLastUpdatedAt(profile))
                .build();
    }

    private Integer calculateAverageResolutionDays(List<BuilderImprovementIssueEntity> issues) {
        List<Long> days = issues.stream()
                .filter(issue -> issue.getStatus() == BuilderImprovementIssueStatus.RESOLVED)
                .filter(issue -> issue.getReportedAt() != null && issue.getResolvedAt() != null)
                .map(issue -> Duration.between(issue.getReportedAt(), issue.getResolvedAt()).toDays())
                .filter(day -> day >= 0)
                .toList();

        if (days.isEmpty()) {
            return null;
        }

        double avg = days.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        return (int) Math.round(avg);
    }

    private List<BuilderImprovementIssueTrendPointResponse> buildTrend(
            List<BuilderImprovementIssueEntity> issues
    ) {
        List<OffsetDateTime> dates = issues.stream()
                .map(this::resolveIssueTimelineDate)
                .filter(date -> date != null)
                .sorted()
                .toList();

        if (dates.isEmpty()) {
            return List.of();
        }

        YearMonth minMonth = YearMonth.from(dates.get(0).toLocalDate());
        YearMonth maxMonth = YearMonth.from(dates.get(dates.size() - 1).toLocalDate());

        int totalMonths = (maxMonth.getYear() - minMonth.getYear()) * 12
                + (maxMonth.getMonthValue() - minMonth.getMonthValue());

        return IntStream.rangeClosed(0, totalMonths)
                .mapToObj(minMonth::plusMonths)
                .map(month -> {
                    long reportedCount = issues.stream()
                            .filter(issue -> isSameMonth(issue.getReportedAt(), month))
                            .count();

                    long resolvedCount = issues.stream()
                            .filter(issue -> isSameMonth(issue.getResolvedAt(), month))
                            .count();

                    long openAtMonthEndCount = issues.stream()
                            .filter(issue -> isOpenAtMonthEnd(issue, month))
                            .count();

                    return BuilderImprovementIssueTrendPointResponse.builder()
                            .monthLabel(month.getMonth().name().substring(0, 3) + " " + month.getYear())
                            .reportedCount(reportedCount)
                            .resolvedCount(resolvedCount)
                            .openAtMonthEndCount(openAtMonthEndCount)
                            .build();
                })
                .toList();
    }

    private OffsetDateTime resolveIssueTimelineDate(BuilderImprovementIssueEntity issue) {
        if (issue.getResolvedAt() != null) {
            return issue.getResolvedAt();
        }

        if (issue.getTargetResolutionDate() != null) {
            return issue.getTargetResolutionDate()
                    .atStartOfDay()
                    .atOffset(OffsetDateTime.now().getOffset());
        }

        return issue.getReportedAt();
    }

    private boolean isSameMonth(OffsetDateTime dateTime, YearMonth month) {
        if (dateTime == null) {
            return false;
        }

        return YearMonth.from(dateTime.toLocalDate()).equals(month);
    }

    private boolean isOpenAtMonthEnd(
            BuilderImprovementIssueEntity issue,
            YearMonth month
    ) {
        if (issue.getStatus() == BuilderImprovementIssueStatus.CANCELLED) {
            return false;
        }

        OffsetDateTime monthEnd = month.atEndOfMonth()
                .atTime(23, 59, 59)
                .atOffset(OffsetDateTime.now().getOffset());

        if (issue.getReportedAt() == null || issue.getReportedAt().isAfter(monthEnd)) {
            return false;
        }

        if (issue.getStatus() == BuilderImprovementIssueStatus.RESOLVED) {
            return issue.getResolvedAt() != null && issue.getResolvedAt().isAfter(monthEnd);
        }

        return issue.getStatus() == BuilderImprovementIssueStatus.PENDING
                || issue.getStatus() == BuilderImprovementIssueStatus.IN_PROGRESS
                || issue.getStatus() == BuilderImprovementIssueStatus.REOPENED;
    }

    private String resolveFooterText(BuilderImprovementProfileEntity profile) {
        if (profile.getVerifiedByText() != null && !profile.getVerifiedByText().isBlank()) {
            return profile.getVerifiedByText();
        }

        return "Data verified by SFS Team";
    }

    private OffsetDateTime resolveLastUpdatedAt(BuilderImprovementProfileEntity profile) {
        if (profile.getLastReviewedAt() != null) {
            return profile.getLastReviewedAt();
        }

        return profile.getUpdatedAt();
    }
}