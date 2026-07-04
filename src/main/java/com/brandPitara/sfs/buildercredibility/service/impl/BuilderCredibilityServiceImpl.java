package com.brandPitara.sfs.buildercredibility.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.buildercredibility.dto.*;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.repository.BuilderHighlightItemRepository;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.brandPitara.sfs.projectmeter.enums.ProjectConstructionStageCode;
import com.brandPitara.sfs.projectmeter.enums.ProjectStageStatus;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuilderCredibilityServiceImpl implements BuilderCredibilityService {

    private static final int EXECUTION_MAX = 30;
    private static final int PROMISE_MAX = 25;
    private static final int COMPLIANCE_MAX = 20;
    private static final int VERIFICATION_MAX = 15;
    private static final int PORTFOLIO_MAX = 10;
    private static final int HOME_CARD_MAX = 10;

    private final BuilderRepository builderRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMeterSnapshotRepository projectMeterSnapshotRepository;
    private final ProjectComplianceItemRepository projectComplianceItemRepository;
    private final ProjectConstructionStageRepository projectConstructionStageRepository;
    private final BuilderHighlightItemRepository builderHighlightItemRepository;

    @Override
    @Transactional(readOnly = true)
    public BuilderCredibilityResponse publicGetCredibility(Long builderId) {
        BuilderEntity builder = getPublicBuilder(builderId);
        BuilderCredibilityComputed computed = computeCredibility(builder);

        return BuilderCredibilityResponse.builder()
            .builderId(builder.getId())
            .builderName(builder.getName())
            .builderLogoUrl(builder.getLogoUrl())
            .cityName(builder.getCity() != null ? builder.getCity().getName() : null)
            .credibilityScore(computed.totalScore)
            .credibilityLabel(computed.label)
            .summary(computed.summary)
            .confidenceLabel(computed.confidenceLabel)
            .trackedProjectsCount(computed.aggregation.trackedProjectsCount)
            .metrics(buildMetrics(computed.aggregation))
            .scoreBreakdown(buildBreakdown(
                computed.executionScore,
                computed.promiseScore,
                computed.complianceScore,
                computed.verificationScore,
                computed.portfolioScore
            ))
            .recentProjectEvidence(buildRecentProjectEvidence(computed.projects, computed.aggregation))
            .positiveIndicators(buildPositiveIndicators(computed.aggregation))
            .observedRisks(buildObservedRisks(computed.aggregation))
            .build();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public BuilderCredibilitySummaryResponse publicGetCredibilitySummary(Long builderId) {
        BuilderEntity builder = getPublicBuilder(builderId);
        BuilderCredibilityComputed computed = computeCredibility(builder);
        Aggregation agg = computed.aggregation;

        double onTrackRecord = agg.trackedProjectsCount == 0
            ? 0.0
            : round2((agg.onTrackProjectCount * 100.0) / agg.trackedProjectsCount);

        double promisesMet = agg.trackedProjectsCount == 0
            ? 0.0
            : round2(agg.totalPromiseFulfilment / (double) agg.trackedProjectsCount);

        double complianceStrength = agg.trackedProjectsCount == 0
            ? 0.0
            : round2(agg.totalComplianceStrength / (double) agg.trackedProjectsCount);

        return BuilderCredibilitySummaryResponse.builder()
            .builderId(builder.getId())
            .builderName(builder.getName())
            .builderLogoUrl(builder.getLogoUrl())
            .cityName(builder.getCity() != null ? builder.getCity().getName() : null)
            .credibilityScore(computed.totalScore)
            .credibilityLabel(computed.label)
            .projectsTrackedCount(agg.trackedProjectsCount)
            .onTrackRecordPercent(onTrackRecord)
            .promisesMetPercent(promisesMet)
            .complianceStrengthPercent(complianceStrength)
            .summary(computed.summary)
            .confidenceLabel(computed.confidenceLabel)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuilderCredibilityCardResponse> publicListCredibilityCards(Long cityId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), HOME_CARD_MAX);

        List<BuilderEntity> builders = (cityId == null)
            ? builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc()
            : builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseAndCity_IdOrderByPriorityAscIdDesc(cityId);

        builders = builders.stream()
            .limit(safeLimit)
            .toList();

        if (builders.isEmpty()) {
            return List.of();
        }

        List<Long> builderIds = builders.stream()
            .map(BuilderEntity::getId)
            .toList();

        List<ProjectEntity> projects = projectRepository
            .findByBuilderIdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(builderIds);

        Map<Long, List<ProjectEntity>> projectsByBuilderId = projects.stream()
            .filter(project -> project.getBuilder() != null && project.getBuilder().getId() != null)
            .collect(Collectors.groupingBy(
                project -> project.getBuilder().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<Long> projectIds = projects.stream()
            .map(ProjectEntity::getId)
            .toList();

        Map<Long, ProjectMeterSnapshotEntity> snapshotMap = new HashMap<>();
        Map<Long, List<ProjectComplianceItemEntity>> complianceMap = new HashMap<>();
        Map<Long, List<ProjectConstructionStageEntity>> stageMap = new HashMap<>();

        if (!projectIds.isEmpty()) {
            snapshotMap = projectMeterSnapshotRepository.findByProjectIdIn(projectIds).stream()
                .collect(Collectors.toMap(s -> s.getProject().getId(), s -> s));

            complianceMap = projectComplianceItemRepository
                .findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(projectIds)
                .stream()
                .collect(Collectors.groupingBy(c -> c.getProject().getId(), LinkedHashMap::new, Collectors.toList()));

            stageMap = projectConstructionStageRepository
                .findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(projectIds)
                .stream()
                .collect(Collectors.groupingBy(s -> s.getProject().getId(), LinkedHashMap::new, Collectors.toList()));
        }

        Map<Long, ProjectMeterSnapshotEntity> finalSnapshotMap = snapshotMap;
        Map<Long, List<ProjectComplianceItemEntity>> finalComplianceMap = complianceMap;
        Map<Long, List<ProjectConstructionStageEntity>> finalStageMap = stageMap;

        return builders.stream()
            .map(builder -> {
                BuilderCredibilityComputed computed = computeCredibility(
                    builder,
                    projectsByBuilderId.getOrDefault(builder.getId(), List.of()),
                    finalSnapshotMap,
                    finalComplianceMap,
                    finalStageMap
                );
                Aggregation agg = computed.aggregation;

                int onTrackRecord = agg.trackedProjectsCount == 0
                    ? 0
                    : Math.round((agg.onTrackProjectCount * 100f) / agg.trackedProjectsCount);

                int promisesMet = agg.trackedProjectsCount == 0
                    ? 0
                    : Math.round(agg.totalPromiseFulfilment / (float) agg.trackedProjectsCount);

                int complianceStrength = agg.trackedProjectsCount == 0
                    ? 0
                    : Math.round(agg.totalComplianceStrength / (float) agg.trackedProjectsCount);

                return BuilderCredibilityCardResponse.builder()
                    .builderId(builder.getId())
                    .builderName(builder.getName())
                    .builderLogoUrl(builder.getLogoUrl())
                    .cityName(builder.getCity() != null ? builder.getCity().getName() : null)
                    .credibilityScore(computed.totalScore)
                    .credibilityLabel(computed.label)
                    .projectsTracked(String.valueOf(agg.trackedProjectsCount))
                    .onTrackRecord(onTrackRecord + "%")
                    .promisesMet(promisesMet + "%")
                    .complianceStrength(complianceStrength + "%")
                    .projectsTrackedCount(agg.trackedProjectsCount)
                    .onTrackRecordPercent(onTrackRecord)
                    .promisesMetPercent(promisesMet)
                    .complianceStrengthPercent(complianceStrength)
                    .summary(computed.summary)
                    .confidenceLabel(computed.confidenceLabel)
                    .highlightsAvailable(hasPublicHighlights(builder.getId()))
                    .highlightCtaLabel("Highlights")
                    .build();
            })
            .toList();
    }

    private boolean hasPublicHighlights(Long builderId) {
        return builderHighlightItemRepository
            .existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
                builderId,
                BuilderHighlightStatus.PUBLISHED
            );
    }

    private BuilderEntity getPublicBuilder(Long builderId) {
        BuilderEntity builder = builderRepository.findByIdAndDeletedFalse(builderId)
            .orElseThrow(() -> new NotFoundException("Builder not found: " + builderId));

        if (!Boolean.TRUE.equals(builder.getPublished()) || !Boolean.TRUE.equals(builder.getActive())) {
            throw new NotFoundException("Builder not found: " + builderId);
        }
        return builder;
    }

    private BuilderCredibilityComputed computeCredibility(BuilderEntity builder) {
        List<ProjectEntity> projects =
            projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(builder.getId());

        List<Long> projectIds = projects.stream()
            .map(ProjectEntity::getId)
            .toList();

        Map<Long, ProjectMeterSnapshotEntity> snapshotMap = new HashMap<>();
        Map<Long, List<ProjectComplianceItemEntity>> complianceMap = new HashMap<>();
        Map<Long, List<ProjectConstructionStageEntity>> stageMap = new HashMap<>();

        if (!projectIds.isEmpty()) {
            snapshotMap = projectMeterSnapshotRepository.findByProjectIdIn(projectIds).stream()
                .collect(Collectors.toMap(s -> s.getProject().getId(), s -> s));

            complianceMap = projectComplianceItemRepository
                .findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(projectIds)
                .stream()
                .collect(Collectors.groupingBy(c -> c.getProject().getId(), LinkedHashMap::new, Collectors.toList()));

            stageMap = projectConstructionStageRepository
                .findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(projectIds)
                .stream()
                .collect(Collectors.groupingBy(s -> s.getProject().getId(), LinkedHashMap::new, Collectors.toList()));
        }

        return computeCredibility(builder, projects, snapshotMap, complianceMap, stageMap);
    }

    private BuilderCredibilityComputed computeCredibility(
        BuilderEntity builder,
        List<ProjectEntity> projects,
        Map<Long, ProjectMeterSnapshotEntity> snapshotMap,
        Map<Long, List<ProjectComplianceItemEntity>> complianceMap,
        Map<Long, List<ProjectConstructionStageEntity>> stageMap
    ) {
        List<ProjectEntity> safeProjects = projects == null ? List.of() : projects;
        Map<Long, ProjectMeterSnapshotEntity> safeSnapshotMap = snapshotMap == null ? Map.of() : snapshotMap;
        Map<Long, List<ProjectComplianceItemEntity>> safeComplianceMap = complianceMap == null ? Map.of() : complianceMap;
        Map<Long, List<ProjectConstructionStageEntity>> safeStageMap = stageMap == null ? Map.of() : stageMap;

        Aggregation aggregation = aggregate(safeProjects, safeSnapshotMap, safeComplianceMap, safeStageMap);

        int executionScore = scoreExecutionReliability(aggregation);
        int promiseScore = scorePromiseFulfilment(aggregation);
        int complianceScore = scoreComplianceStrength(aggregation);
        int verificationScore = scoreVerificationConfidence(aggregation);
        int portfolioScore = scorePortfolioMaturity(aggregation);

        int totalScore = clamp(
            executionScore + promiseScore + complianceScore + verificationScore + portfolioScore,
            0,
            100
        );

        String label = credibilityLabel(totalScore);

        return BuilderCredibilityComputed.builder()
            .projects(safeProjects)
            .aggregation(aggregation)
            .executionScore(executionScore)
            .promiseScore(promiseScore)
            .complianceScore(complianceScore)
            .verificationScore(verificationScore)
            .portfolioScore(portfolioScore)
            .totalScore(totalScore)
            .label(label)
            .confidenceLabel(confidenceLabel(aggregation))
            .summary(buildSummary(label, aggregation))
            .build();
    }

    private Aggregation aggregate(
        List<ProjectEntity> projects,
        Map<Long, ProjectMeterSnapshotEntity> snapshotMap,
        Map<Long, List<ProjectComplianceItemEntity>> complianceMap,
        Map<Long, List<ProjectConstructionStageEntity>> stageMap
    ) {
        Aggregation agg = new Aggregation();
        agg.trackedProjectsCount = projects.size();

        for (ProjectEntity project : projects) {
            Long projectId = project.getId();
            ProjectMeterSnapshotEntity snapshot = snapshotMap.get(projectId);
            List<ProjectComplianceItemEntity> complianceItems = complianceMap.getOrDefault(projectId, List.of());
            List<ProjectConstructionStageEntity> stages = stageMap.getOrDefault(projectId, List.of());

            if (snapshot != null) {
                agg.snapshotCoverageCount++;
                if (Boolean.TRUE.equals(snapshot.getVerified())) {
                    agg.verifiedSnapshotCount++;
                }
            }

            int progress = snapshot != null && snapshot.getConstructionProgressPercent() != null
                ? clamp(snapshot.getConstructionProgressPercent(), 0, 100)
                : calculateProgressFromStages(stages);

            int delayDays = snapshot != null && snapshot.getDelayDays() != null
                ? Math.max(snapshot.getDelayDays(), 0)
                : 0;

            if (delayDays <= 0) {
                agg.onTrackProjectCount++;
            } else {
                agg.delayedProjectCount++;
                agg.totalDelayDays += delayDays;
            }

            agg.totalProgress += progress;

            int projectCompliancePercent = calculateComplianceStrengthPercent(complianceItems);
            agg.totalComplianceStrength += projectCompliancePercent;

            int projectPromisePercent = calculatePromiseFulfilmentPercent(delayDays, complianceItems, stages);
            agg.totalPromiseFulfilment += projectPromisePercent;

            int projectVerificationPercent = calculateVerificationPercent(snapshot, complianceItems, stages);
            agg.totalVerificationStrength += projectVerificationPercent;

            if (!stages.isEmpty()) {
                agg.projectsWithStagesCount++;
            }
            if (!complianceItems.isEmpty()) {
                agg.projectsWithComplianceCount++;
            }

            agg.projectEvidence.add(ProjectEvidenceInternal.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .projectSlug(project.getSlug())
                .cityName(project.getCity() != null ? project.getCity().getName() : null)
                .constructionProgressPercent(progress)
                .delayDays(delayDays)
                .timelineStatus(delayDays > 0 ? "DELAYED" : "ON_TRACK")
                .promiseFulfilmentPercent(projectPromisePercent)
                .complianceStrengthPercent(projectCompliancePercent)
                .verified(projectVerificationPercent >= 70)
                .priority(project.getPriority() != null ? project.getPriority() : 0)
                .build());
        }

        return agg;
    }

    private List<BuilderCredibilityMetricDto> buildMetrics(Aggregation agg) {
        int onTrackRecord = agg.trackedProjectsCount == 0
            ? 0
            : Math.round((agg.onTrackProjectCount * 100f) / agg.trackedProjectsCount);

        int avgDelay = agg.delayedProjectCount == 0
            ? 0
            : Math.round(agg.totalDelayDays / (float) agg.delayedProjectCount);

        int promisesMet = agg.trackedProjectsCount == 0
            ? 0
            : Math.round(agg.totalPromiseFulfilment / (float) agg.trackedProjectsCount);

        int complianceStrength = agg.trackedProjectsCount == 0
            ? 0
            : Math.round(agg.totalComplianceStrength / (float) agg.trackedProjectsCount);

        return List.of(
            BuilderCredibilityMetricDto.builder()
                .key("projects_tracked")
                .label("Projects Tracked")
                .value(String.valueOf(agg.trackedProjectsCount))
                .subtitle(agg.snapshotCoverageCount + " with meter evidence")
                .build(),
            BuilderCredibilityMetricDto.builder()
                .key("on_track_record")
                .label("On-Track Record")
                .value(onTrackRecord + "%")
                .subtitle(avgDelay > 0 ? "Avg delay " + avgDelay + " days" : "No active delay detected")
                .build(),
            BuilderCredibilityMetricDto.builder()
                .key("promises_met")
                .label("Promises Met")
                .value(promisesMet + "%")
                .subtitle("Derived from timelines, stages, handover readiness and compliance")
                .build(),
            BuilderCredibilityMetricDto.builder()
                .key("compliance_strength")
                .label("Compliance Strength")
                .value(complianceStrength + "%")
                .subtitle(agg.projectsWithComplianceCount + " projects with compliance evidence")
                .build()
        );
    }

    private List<BuilderCredibilityScoreBreakdownDto> buildBreakdown(
        int executionScore,
        int promiseScore,
        int complianceScore,
        int verificationScore,
        int portfolioScore
    ) {
        return List.of(
            BuilderCredibilityScoreBreakdownDto.builder()
                .key("execution_reliability")
                .label("Execution Reliability")
                .score(executionScore)
                .maxScore(EXECUTION_MAX)
                .summary("Based on construction progress, on-track ratio and delay severity.")
                .build(),
            BuilderCredibilityScoreBreakdownDto.builder()
                .key("promise_fulfilment")
                .label("Promise Fulfilment")
                .score(promiseScore)
                .maxScore(PROMISE_MAX)
                .summary("Derived from stage discipline, handover readiness, delay impact and compliance completion.")
                .build(),
            BuilderCredibilityScoreBreakdownDto.builder()
                .key("compliance_strength")
                .label("Compliance Strength")
                .score(complianceScore)
                .maxScore(COMPLIANCE_MAX)
                .summary("Measures approvals, verification quality and negative compliance signals.")
                .build(),
            BuilderCredibilityScoreBreakdownDto.builder()
                .key("verification_confidence")
                .label("Verification Confidence")
                .score(verificationScore)
                .maxScore(VERIFICATION_MAX)
                .summary("Reflects how much of the tracked portfolio has verified evidence.")
                .build(),
            BuilderCredibilityScoreBreakdownDto.builder()
                .key("portfolio_maturity")
                .label("Portfolio Maturity")
                .score(portfolioScore)
                .maxScore(PORTFOLIO_MAX)
                .summary("Rewards broader tracked portfolio coverage and meter-backed evidence.")
                .build()
        );
    }

    private List<BuilderCredibilityProjectEvidenceDto> buildRecentProjectEvidence(
        List<ProjectEntity> projects,
        Aggregation agg
    ) {
        Map<Long, Integer> projectOrder = new HashMap<>();
        for (int i = 0; i < projects.size(); i++) {
            projectOrder.put(projects.get(i).getId(), i);
        }

        return agg.projectEvidence.stream()
            .sorted(Comparator
                .comparingInt((ProjectEvidenceInternal p) -> p.priority)
                .thenComparingInt(p -> projectOrder.getOrDefault(p.projectId, Integer.MAX_VALUE)))
            .limit(5)
            .map(p -> BuilderCredibilityProjectEvidenceDto.builder()
                .projectId(p.projectId)
                .projectName(p.projectName)
                .projectSlug(p.projectSlug)
                .cityName(p.cityName)
                .constructionProgressPercent(p.constructionProgressPercent)
                .timelineStatus(p.timelineStatus)
                .delayDays(p.delayDays)
                .promiseFulfilmentPercent(p.promiseFulfilmentPercent)
                .complianceStrengthPercent(p.complianceStrengthPercent)
                .verified(p.verified)
                .build())
            .toList();
    }

    private List<BuilderCredibilityIndicatorDto> buildPositiveIndicators(Aggregation agg) {
        List<BuilderCredibilityIndicatorDto> items = new ArrayList<>();

        if (agg.trackedProjectsCount > 0) {
            int onTrackPercent = Math.round((agg.onTrackProjectCount * 100f) / agg.trackedProjectsCount);
            if (onTrackPercent >= 70) {
                items.add(BuilderCredibilityIndicatorDto.builder()
                    .title("Strong on-track execution")
                    .description(onTrackPercent + "% of tracked projects are currently on track.")
                    .build());
            }
        }

        if (agg.snapshotCoverageCount > 0) {
            int verifiedCoverage = Math.round((agg.verifiedSnapshotCount * 100f) / agg.snapshotCoverageCount);
            if (verifiedCoverage >= 60) {
                items.add(BuilderCredibilityIndicatorDto.builder()
                    .title("Good verified evidence coverage")
                    .description(verifiedCoverage + "% of tracked meter snapshots are verified.")
                    .build());
            }
        }

        if (agg.trackedProjectsCount > 0) {
            int complianceStrength = Math.round(agg.totalComplianceStrength / (float) agg.trackedProjectsCount);
            if (complianceStrength >= 70) {
                items.add(BuilderCredibilityIndicatorDto.builder()
                    .title("Healthy compliance profile")
                    .description("Portfolio shows strong approval and compliance signal quality.")
                    .build());
            }
        }

        if (items.isEmpty()) {
            items.add(BuilderCredibilityIndicatorDto.builder()
                .title("Portfolio under observation")
                .description("Builder credibility is being assessed from available project-meter evidence.")
                .build());
        }

        return items;
    }

    private List<BuilderCredibilityRiskDto> buildObservedRisks(Aggregation agg) {
        List<BuilderCredibilityRiskDto> items = new ArrayList<>();

        if (agg.delayedProjectCount > 0) {
            int avgDelay = Math.round(agg.totalDelayDays / (float) agg.delayedProjectCount);
            items.add(BuilderCredibilityRiskDto.builder()
                .title("Delay pressure detected")
                .description(agg.delayedProjectCount + " tracked projects show active delay, with average delay around " + avgDelay + " days.")
                .build());
        }

        if (agg.snapshotCoverageCount < agg.trackedProjectsCount) {
            items.add(BuilderCredibilityRiskDto.builder()
                .title("Incomplete meter coverage")
                .description("Some published projects do not yet have full meter snapshot evidence.")
                .build());
        }

        if (agg.projectsWithComplianceCount < agg.trackedProjectsCount) {
            items.add(BuilderCredibilityRiskDto.builder()
                .title("Compliance evidence gaps")
                .description("Not every tracked project currently exposes strong compliance documentation.")
                .build());
        }

        if (items.isEmpty()) {
            items.add(BuilderCredibilityRiskDto.builder()
                .title("No major portfolio-level risk flag")
                .description("Current tracked evidence does not show a strong builder-level risk concentration.")
                .build());
        }

        return items;
    }

    private int scoreExecutionReliability(Aggregation agg) {
        if (agg.trackedProjectsCount == 0) {
            return 0;
        }

        float avgProgress = agg.totalProgress / (float) agg.trackedProjectsCount;
        float onTrackRatio = agg.onTrackProjectCount / (float) agg.trackedProjectsCount;
        float delayPenaltyRatio = agg.delayedProjectCount == 0
            ? 0f
            : Math.min(1f, (agg.totalDelayDays / (float) agg.delayedProjectCount) / 180f);

        float normalized = (avgProgress * 0.45f)
            + (onTrackRatio * 100f * 0.45f)
            + ((1f - delayPenaltyRatio) * 100f * 0.10f);

        return scaleToWeight(normalized, EXECUTION_MAX);
    }

    private int scorePromiseFulfilment(Aggregation agg) {
        if (agg.trackedProjectsCount == 0) {
            return 0;
        }

        float avgPromise = agg.totalPromiseFulfilment / (float) agg.trackedProjectsCount;
        return scaleToWeight(avgPromise, PROMISE_MAX);
    }

    private int scoreComplianceStrength(Aggregation agg) {
        if (agg.trackedProjectsCount == 0) {
            return 0;
        }

        float avgCompliance = agg.totalComplianceStrength / (float) agg.trackedProjectsCount;
        return scaleToWeight(avgCompliance, COMPLIANCE_MAX);
    }

    private int scoreVerificationConfidence(Aggregation agg) {
        if (agg.trackedProjectsCount == 0) {
            return 0;
        }

        float avgVerification = agg.totalVerificationStrength / (float) agg.trackedProjectsCount;
        return scaleToWeight(avgVerification, VERIFICATION_MAX);
    }

    private int scorePortfolioMaturity(Aggregation agg) {
        if (agg.trackedProjectsCount == 0) {
            return 0;
        }

        float projectDepth = Math.min(100f, agg.trackedProjectsCount * 20f);
        float snapshotCoverage = agg.trackedProjectsCount == 0
            ? 0f
            : (agg.snapshotCoverageCount * 100f / (float) agg.trackedProjectsCount);

        float normalized = (projectDepth * 0.55f) + (snapshotCoverage * 0.45f);
        return scaleToWeight(normalized, PORTFOLIO_MAX);
    }

    private int calculateComplianceStrengthPercent(List<ProjectComplianceItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return 35;
        }

        double total = 0.0;
        for (ProjectComplianceItemEntity item : items) {
            total += complianceStatusScore(item.getStatus(), item.getVerified());
        }

        return clamp((int) Math.round(total / items.size()), 0, 100);
    }

    private int calculatePromiseFulfilmentPercent(
        int delayDays,
        List<ProjectComplianceItemEntity> complianceItems,
        List<ProjectConstructionStageEntity> stages
    ) {
        double timelineScore = 100.0 - Math.min(delayDays, 240) * (100.0 / 240.0);

        double stageDisciplineScore = 50.0;
        if (stages != null && !stages.isEmpty()) {
            long delayed = stages.stream().filter(s -> s.getStatus() == ProjectStageStatus.DELAYED).count();
            long completed = stages.stream().filter(s -> s.getStatus() == ProjectStageStatus.COMPLETED).count();

            double delayedRatioPenalty = (delayed * 100.0) / stages.size();
            double completedRatioBoost = (completed * 100.0) / stages.size();

            stageDisciplineScore = clampDouble(
                (completedRatioBoost * 0.6) + ((100.0 - delayedRatioPenalty) * 0.4),
                0,
                100
            );
        }

        double handoverReadinessScore = 40.0;
        Optional<ProjectConstructionStageEntity> handoverStage = stages == null
            ? Optional.empty()
            : stages.stream()
                .filter(s -> s.getStageCode() == ProjectConstructionStageCode.HANDOVER_READINESS)
                .findFirst();

        if (handoverStage.isPresent()) {
            ProjectConstructionStageEntity stage = handoverStage.get();
            if (stage.getStatus() == ProjectStageStatus.COMPLETED) {
                handoverReadinessScore = 100.0;
            } else if (stage.getStatus() == ProjectStageStatus.IN_PROGRESS) {
                handoverReadinessScore = 65.0;
            } else if (stage.getStatus() == ProjectStageStatus.DELAYED) {
                handoverReadinessScore = 20.0;
            } else {
                handoverReadinessScore = 35.0;
            }
        }

        double complianceSupport = calculateComplianceStrengthPercent(complianceItems);

        double promiseScore = (timelineScore * 0.40)
            + (stageDisciplineScore * 0.25)
            + (handoverReadinessScore * 0.15)
            + (complianceSupport * 0.20);

        return clamp((int) Math.round(promiseScore), 0, 100);
    }

    private int calculateVerificationPercent(
        ProjectMeterSnapshotEntity snapshot,
        List<ProjectComplianceItemEntity> complianceItems,
        List<ProjectConstructionStageEntity> stages
    ) {
        double snapshotScore = snapshot != null && Boolean.TRUE.equals(snapshot.getVerified()) ? 100.0 : (snapshot != null ? 50.0 : 0.0);

        double stageVerifiedScore = 0.0;
        if (stages != null && !stages.isEmpty()) {
            long verifiedCount = stages.stream().filter(s -> Boolean.TRUE.equals(s.getVerified())).count();
            stageVerifiedScore = (verifiedCount * 100.0) / stages.size();
        }

        double complianceVerifiedScore = 0.0;
        if (complianceItems != null && !complianceItems.isEmpty()) {
            long verifiedCount = complianceItems.stream().filter(c -> Boolean.TRUE.equals(c.getVerified())).count();
            complianceVerifiedScore = (verifiedCount * 100.0) / complianceItems.size();
        }

        double total = (snapshotScore * 0.40) + (stageVerifiedScore * 0.30) + (complianceVerifiedScore * 0.30);
        return clamp((int) Math.round(total), 0, 100);
    }

    private int calculateProgressFromStages(List<ProjectConstructionStageEntity> stages) {
        if (stages == null || stages.isEmpty()) {
            return 0;
        }

        int weightedSum = 0;
        int totalWeight = 0;

        for (ProjectConstructionStageEntity stage : stages) {
            int progress = clamp(stage.getProgressPercent() != null ? stage.getProgressPercent() : 0, 0, 100);
            int weight = Math.max(stage.getWeightPercent() != null ? stage.getWeightPercent() : 0, 0);

            weightedSum += progress * weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return 0;
        }

        return Math.round(weightedSum / (float) totalWeight);
    }

    private double complianceStatusScore(ProjectComplianceStatus status, Boolean verified) {
        double base = switch (status) {
            case OBTAINED, VERIFIED, APPROVED -> 90.0;
            case PENDING, SUBMITTED -> 40.0;
            case NOT_APPLICABLE -> 100.0;
            case EXPIRED -> 10.0;
        };

        if (Boolean.TRUE.equals(verified)) {
            base = Math.min(100.0, base + 5.0);
        }

        return base;
    }

    private String credibilityLabel(int score) {
        if (score >= 90) return "Highly Reliable";
        if (score >= 75) return "Reliable";
        if (score >= 60) return "Moderate Confidence";
        if (score >= 40) return "Needs Caution";
        return "High Risk";
    }

    private String confidenceLabel(Aggregation agg) {
        if (agg.trackedProjectsCount >= 5 && agg.snapshotCoverageCount >= Math.max(3, Math.round(agg.trackedProjectsCount * 0.7f))) {
            return "High";
        }
        if (agg.trackedProjectsCount >= 2 && agg.snapshotCoverageCount >= 1) {
            return "Medium";
        }
        return "Low";
    }

    private String buildSummary(String label, Aggregation agg) {
        if (agg.trackedProjectsCount == 0) {
            return "Credibility is currently based on builder profile visibility only because no tracked public projects were found.";
        }

        int onTrackPercent = Math.round((agg.onTrackProjectCount * 100f) / agg.trackedProjectsCount);
        int promisePercent = Math.round(agg.totalPromiseFulfilment / (float) agg.trackedProjectsCount);

        return label + " based on " + agg.trackedProjectsCount
            + " tracked public projects, with " + onTrackPercent
            + "% currently on track and derived promise fulfilment of " + promisePercent + "%.";
    }

    private int scaleToWeight(float normalizedPercent, int weight) {
        return clamp(Math.round((normalizedPercent / 100f) * weight), 0, weight);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class Aggregation {
        private int trackedProjectsCount;
        private int snapshotCoverageCount;
        private int verifiedSnapshotCount;
        private int onTrackProjectCount;
        private int delayedProjectCount;
        private int totalDelayDays;
        private int totalProgress;
        private int totalComplianceStrength;
        private int totalPromiseFulfilment;
        private int totalVerificationStrength;
        private int projectsWithStagesCount;
        private int projectsWithComplianceCount;
        private final List<ProjectEvidenceInternal> projectEvidence = new ArrayList<>();
    }

    @lombok.Builder
    private static class ProjectEvidenceInternal {
        private Long projectId;
        private String projectName;
        private String projectSlug;
        private String cityName;
        private Integer constructionProgressPercent;
        private String timelineStatus;
        private Integer delayDays;
        private Integer promiseFulfilmentPercent;
        private Integer complianceStrengthPercent;
        private Boolean verified;
        private Integer priority;
    }

    @lombok.Builder
    private static class BuilderCredibilityComputed {
        private List<ProjectEntity> projects;
        private Aggregation aggregation;
        private Integer executionScore;
        private Integer promiseScore;
        private Integer complianceScore;
        private Integer verificationScore;
        private Integer portfolioScore;
        private Integer totalScore;
        private String label;
        private String confidenceLabel;
        private String summary;
    }
}
