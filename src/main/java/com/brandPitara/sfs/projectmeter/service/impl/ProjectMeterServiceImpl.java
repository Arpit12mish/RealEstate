package com.brandPitara.sfs.projectmeter.service.impl;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectApprovalsResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectComplianceGroupResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectComplianceItemResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectConstructionProgressResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectConstructionStageResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectCostBreakdownResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectLandLicenseResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectLandUtilizationResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectLocationRadarResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectPaymentMilestoneResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectPriceHistoryPointResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectPriceInsightsResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectTimelineResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectCostBreakdownEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectLandUtilizationEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectLocationScoreEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectPaymentMilestoneEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectPriceHistoryEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterMapper;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterProjectMapper;
import com.brandPitara.sfs.projectmeter.mapper.ProjectAmenitiesAssembler;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectCostBreakdownRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLandUtilizationRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLocationScoreRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectPaymentMilestoneRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectPriceHistoryRepository;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import com.brandPitara.sfs.projectmeter.service.reader.ProjectMeterFavoriteReader;
import com.brandPitara.sfs.projectmeter.service.reader.ProjectMeterSupplementalReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMeterServiceImpl implements ProjectMeterService {

    private final ProjectRepository projectRepository;
    private final ProjectMeterSnapshotRepository projectMeterSnapshotRepository;
    private final ProjectConstructionStageRepository projectConstructionStageRepository;
    private final ProjectComplianceItemRepository projectComplianceItemRepository;
    private final ProjectPriceHistoryRepository projectPriceHistoryRepository;
    private final ProjectPaymentMilestoneRepository projectPaymentMilestoneRepository;
    private final ProjectCostBreakdownRepository projectCostBreakdownRepository;
    private final ProjectLandUtilizationRepository projectLandUtilizationRepository;
    private final ProjectLocationScoreRepository projectLocationScoreRepository;
    private final ProjectAmenityProgressRepository projectAmenityProgressRepository;
    private final ProjectMediaRepository projectMediaRepository;
    private final BuilderCredibilityService builderCredibilityService;
    private final ProjectFavoriteService projectFavoriteService;
    private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
    private final ProjectMeterSupplementalReader projectMeterSupplementalReader;
    private final ProjectMeterFavoriteReader projectMeterFavoriteReader;
    private final ProjectAmenitiesAssembler projectAmenitiesAssembler;

    private record ComplianceSections(
        List<ProjectComplianceItemEntity> landLicenseItems,
        List<ProjectComplianceItemEntity> approvalItems,
        List<ProjectComplianceGroupResponse> allGroups
    ) {
    }

    private ComplianceSections loadComplianceSections(Long projectId) {
        List<ProjectComplianceItemEntity> complianceItems =
            projectComplianceItemRepository
                .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(projectId);

        Map<ProjectComplianceGroup, List<ProjectComplianceItemEntity>>
            itemsByGroup = complianceItems.stream()
                .collect(Collectors.groupingBy(
                    ProjectComplianceItemEntity::getItemGroup,
                    () -> new java.util.EnumMap<>(ProjectComplianceGroup.class),
                    Collectors.toList()
                ));

        // EnumMap iterates in the enum's declaration order, so every group
        // below (not just LAND_LICENSE/APPROVAL_NOC) reaches the public API
        // — items tagged with any other group used to be silently dropped.
        List<ProjectComplianceGroupResponse> allGroups = itemsByGroup.entrySet().stream()
            .map(entry -> ProjectComplianceGroupResponse.builder()
                .group(entry.getKey().name())
                .groupLabel(complianceGroupLabel(entry.getKey()))
                .items(entry.getValue().stream().map(this::toComplianceItemResponse).toList())
                .build())
            .toList();

        return new ComplianceSections(
            itemsByGroup.getOrDefault(
                ProjectComplianceGroup.LAND_LICENSE,
                List.of()
            ),
            itemsByGroup.getOrDefault(
                ProjectComplianceGroup.APPROVAL_NOC,
                List.of()
            ),
            allGroups
        );
    }

    private static String complianceGroupLabel(ProjectComplianceGroup group) {
        return switch (group) {
            case RERA -> "RERA";
            case ENVIRONMENTAL -> "Environmental";
            case FIRE_SAFETY -> "Fire Safety";
            case STRUCTURAL -> "Structural";
            case LEGAL_TITLE -> "Legal Title";
            case UTILITY -> "Utility";
            case OCCUPANCY -> "Occupancy";
            case OTHER -> "Other";
            case LAND_LICENSE -> "Land License";
            case APPROVAL_NOC -> "Approval / NOC";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMeterSummaryResponse publicGetMeterSummary(Long projectId) {
        ProjectEntity project = getPublicProject(projectId);

        return projectMeterSnapshotRepository.findByProjectId(projectId)
            .map(ProjectMeterMapper::toSummaryResponse)
            .orElseGet(() -> buildFallbackSummary(project));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMeterSummaryResponse adminGetMeterSummary(Long projectId) {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        return projectMeterSnapshotRepository.findByProjectId(projectId)
                .map(ProjectMeterMapper::toSummaryResponse)
                .orElseGet(() -> buildFallbackSummary(project));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectConstructionProgressResponse publicGetConstructionProgress(Long projectId) {
        ProjectEntity project = getPublicProject(projectId);

        List<ProjectConstructionStageEntity> stages =
            projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        ProjectMeterSnapshotEntity snapshot = projectMeterSnapshotRepository.findByProjectId(projectId).orElse(null);

        int overallProgress = snapshot != null && snapshot.getConstructionProgressPercent() != null
            ? safePercent(snapshot.getConstructionProgressPercent())
            : calculateOverallProgress(stages);

        int delayDays = snapshot != null && snapshot.getDelayDays() != null
            ? Math.max(snapshot.getDelayDays(), 0)
            : calculateDelayDays(project, snapshot);

        List<ProjectConstructionStageResponse> stageResponses = stages.stream()
            .map(ProjectMeterMapper::toStageResponse)
            .toList();

        return ProjectConstructionProgressResponse.builder()
            .projectId(project.getId())
            .overallProgressPercent(overallProgress)
            .delayDays(delayDays)
            .timeline(resolveTimeline(project, snapshot))
            .stages(stageResponses)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMeterDetailResponse publicGetMeterDetail(Long projectId) {
        ProjectEntity project = projectRepository.findDetailByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

        ProjectMeterSupplementalReader.SupplementalSections supplemental =
            projectMeterSupplementalReader.read(project);
        ProjectMeterFavoriteReader.FavoriteState favorite = projectMeterFavoriteReader.read(projectId);

        ProjectMeterSnapshotEntity snapshot = projectMeterSnapshotRepository.findByProjectId(projectId).orElse(null);
        List<ProjectConstructionStageEntity> stages =
            projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        ProjectMeterSummaryResponse summary = (snapshot != null)
            ? ProjectMeterMapper.toSummaryResponse(snapshot)
            : buildFallbackSummary(project, stages);

        int overallProgress = snapshot != null && snapshot.getConstructionProgressPercent() != null
            ? safePercent(snapshot.getConstructionProgressPercent())
            : calculateOverallProgress(stages);

        int delayDays = snapshot != null && snapshot.getDelayDays() != null
            ? Math.max(snapshot.getDelayDays(), 0)
            : calculateDelayDays(project, snapshot);

        ProjectConstructionProgressResponse construction = ProjectConstructionProgressResponse.builder()
            .projectId(project.getId())
            .overallProgressPercent(overallProgress)
            .delayDays(delayDays)
            .timeline(resolveTimeline(project, snapshot))
            .stages(stages.stream().map(ProjectMeterMapper::toStageResponse).toList())
            .build();

        ComplianceSections compliance = loadComplianceSections(projectId);

        ProjectLandLicenseResponse landLicense = ProjectLandLicenseResponse.builder()
            .items(
                compliance.landLicenseItems()
                    .stream()
                    .map(this::toComplianceItemResponse)
                    .toList()
            )
            .build();

        ProjectApprovalsResponse approvals = ProjectApprovalsResponse.builder()
            .items(
                compliance.approvalItems()
                    .stream()
                    .map(this::toComplianceItemResponse)
                    .toList()
            )
            .build();

        ProjectPriceInsightsResponse priceInsights = ProjectPriceInsightsResponse.builder()
            .launchPrice(snapshot != null ? snapshot.getLaunchPrice() : null)
            .currentPrice(snapshot != null ? snapshot.getCurrentPrice() : null)
            .appreciationPercent(snapshot != null ? snapshot.getPriceAppreciationPercent() : null)
            .averageAreaPrice(snapshot != null ? snapshot.getAverageAreaPrice() : null)
            .build();

        List<ProjectPriceHistoryPointResponse> propertyRates = projectPriceHistoryRepository
            .findByProjectIdOrderByDisplayOrderAscIdAsc(projectId)
            .stream()
            .map(this::toPriceHistoryPointResponse)
            .toList();

        List<ProjectPaymentMilestoneResponse> paymentPlan = projectPaymentMilestoneRepository
            .findByProjectIdAndActiveTrueOrderByDisplayOrderAscIdAsc(projectId)
            .stream()
            .map(this::toPaymentMilestoneResponse)
            .toList();

        ProjectCostBreakdownResponse estimatedCost = projectCostBreakdownRepository.findByProjectId(projectId)
            .map(this::toCostBreakdownResponse)
            .orElseGet(() -> ProjectCostBreakdownResponse.builder()
                .totalCost(snapshot != null ? snapshot.getEstimatedCostTotal() : null)
                .landCost(null)
                .constructionCost(null)
                .infrastructureCost(null)
                .otherCost(null)
                .sourceLabel(null)
                .remarks(null)
                .verified(false)
                .build());

        ProjectLandUtilizationResponse landUtilization = projectLandUtilizationRepository.findByProjectId(projectId)
            .map(this::toLandUtilizationResponse)
            .orElseGet(() -> ProjectLandUtilizationResponse.builder().build());

        ProjectLocationRadarResponse locationRadar = projectLocationScoreRepository.findByProjectId(projectId)
            .map(this::toLocationRadarResponse)
            .orElseGet(() -> ProjectLocationRadarResponse.builder()
                .finalScore(snapshot != null ? snapshot.getLocationScore() : null)
                .appreciationPercent3Y(snapshot != null ? snapshot.getLocationAppreciationPercent3Y() : null)
                .verified(snapshot != null ? snapshot.getVerified() : false)
                .build());

        List<ProjectAmenityProgressEntity> publicAmenities =
            projectAmenityProgressRepository
                .findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(projectId);

        ProjectAmenitiesResponse amenities = projectAmenitiesAssembler.assemble(
            publicAmenities,
            snapshot != null ? snapshot.getAmenityScore() : null
        );

        BuilderCredibilitySummaryResponse builderCredibility = safeBuilderCredibility(project);

        return ProjectMeterDetailResponse.builder()
            .project(ProjectMeterProjectMapper.toResponse(
                project,
                supplemental.brochureUrl(),
                favorite.favorite(),
                favorite.count()
            ))
            .media(supplemental.media())
            .summary(summary)
            .construction(construction)
            .landLicense(landLicense)
            .approvals(approvals)
            .complianceGroups(compliance.allGroups())
            .priceInsights(priceInsights)
            .propertyRates(propertyRates)
            .paymentPlan(paymentPlan)
            .estimatedCost(estimatedCost)
            .landUtilization(landUtilization)
            .locationRadar(locationRadar)
            .amenities(amenities)
            .builderCredibility(builderCredibility)
            .masterPlan(supplemental.masterPlan())
            .floorPlanGroups(supplemental.floorPlanGroups())
            .connectivity(supplemental.connectivity())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMeterDetailResponse dashboardGetMeterDetail(Long projectId) {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        ProjectMeterSnapshotEntity snapshot = projectMeterSnapshotRepository.findByProjectId(projectId).orElse(null);
        List<ProjectConstructionStageEntity> stages =
            projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        ProjectMeterSummaryResponse summary = (snapshot != null)
            ? ProjectMeterMapper.toSummaryResponse(snapshot)
            : buildFallbackSummary(project, stages);

        int overallProgress = snapshot != null && snapshot.getConstructionProgressPercent() != null
            ? safePercent(snapshot.getConstructionProgressPercent())
            : calculateOverallProgress(stages);

        int delayDays = snapshot != null && snapshot.getDelayDays() != null
            ? Math.max(snapshot.getDelayDays(), 0)
            : calculateDelayDays(project, snapshot);

        ProjectConstructionProgressResponse construction = ProjectConstructionProgressResponse.builder()
            .projectId(project.getId())
            .overallProgressPercent(overallProgress)
            .delayDays(delayDays)
            .timeline(resolveTimeline(project, snapshot))
            .stages(stages.stream().map(ProjectMeterMapper::toStageResponse).toList())
            .build();

        ComplianceSections compliance = loadComplianceSections(projectId);

        ProjectLandLicenseResponse landLicense = ProjectLandLicenseResponse.builder()
            .items(
                compliance.landLicenseItems()
                    .stream()
                    .map(this::toComplianceItemResponse)
                    .toList()
            )
            .build();

        ProjectApprovalsResponse approvals = ProjectApprovalsResponse.builder()
            .items(
                compliance.approvalItems()
                    .stream()
                    .map(this::toComplianceItemResponse)
                    .toList()
            )
            .build();

        ProjectPriceInsightsResponse priceInsights = ProjectPriceInsightsResponse.builder()
            .launchPrice(snapshot != null ? snapshot.getLaunchPrice() : null)
            .currentPrice(snapshot != null ? snapshot.getCurrentPrice() : null)
            .appreciationPercent(snapshot != null ? snapshot.getPriceAppreciationPercent() : null)
            .averageAreaPrice(snapshot != null ? snapshot.getAverageAreaPrice() : null)
            .build();

        List<ProjectPriceHistoryPointResponse> propertyRates = projectPriceHistoryRepository
            .findByProjectIdOrderByDisplayOrderAscIdAsc(projectId)
            .stream()
            .map(this::toPriceHistoryPointResponse)
            .toList();

        List<ProjectPaymentMilestoneResponse> paymentPlan = projectPaymentMilestoneRepository
            .findByProjectIdAndActiveTrueOrderByDisplayOrderAscIdAsc(projectId)
            .stream()
            .map(this::toPaymentMilestoneResponse)
            .toList();

        ProjectCostBreakdownResponse estimatedCost = projectCostBreakdownRepository.findByProjectId(projectId)
            .map(this::toCostBreakdownResponse)
            .orElseGet(() -> ProjectCostBreakdownResponse.builder()
                .totalCost(snapshot != null ? snapshot.getEstimatedCostTotal() : null)
                .landCost(null)
                .constructionCost(null)
                .infrastructureCost(null)
                .otherCost(null)
                .sourceLabel(null)
                .remarks(null)
                .verified(false)
                .build());

        ProjectLandUtilizationResponse landUtilization = projectLandUtilizationRepository.findByProjectId(projectId)
            .map(this::toLandUtilizationResponse)
            .orElseGet(() -> ProjectLandUtilizationResponse.builder().build());

        ProjectLocationRadarResponse locationRadar = projectLocationScoreRepository.findByProjectId(projectId)
            .map(this::toLocationRadarResponse)
            .orElseGet(() -> ProjectLocationRadarResponse.builder()
                .finalScore(snapshot != null ? snapshot.getLocationScore() : null)
                .appreciationPercent3Y(snapshot != null ? snapshot.getLocationAppreciationPercent3Y() : null)
                .verified(snapshot != null ? snapshot.getVerified() : false)
                .build());

        List<ProjectAmenityProgressEntity> publicAmenities =
            projectAmenityProgressRepository
                .findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(projectId);

        ProjectAmenitiesResponse amenities = projectAmenitiesAssembler.assemble(
            publicAmenities,
            snapshot != null ? snapshot.getAmenityScore() : null
        );

        BuilderCredibilitySummaryResponse builderCredibility = safeBuilderCredibility(project);

        return ProjectMeterDetailResponse.builder()
            .summary(summary)
            .construction(construction)
            .landLicense(landLicense)
            .approvals(approvals)
            .complianceGroups(compliance.allGroups())
            .priceInsights(priceInsights)
            .propertyRates(propertyRates)
            .paymentPlan(paymentPlan)
            .estimatedCost(estimatedCost)
            .landUtilization(landUtilization)
            .locationRadar(locationRadar)
            .amenities(amenities)
            .builderCredibility(builderCredibility)
            .build();
    }

    private ProjectEntity getPublicProject(Long projectId) {
        ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        projectPublicVisibilityPolicy.assertPubliclyVisible(entity, projectId);
        return entity;
    }

    private ProjectMeterSummaryResponse buildFallbackSummary(ProjectEntity project) {
        List<ProjectConstructionStageEntity> stages =
            projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(project.getId());

        return buildFallbackSummary(project, stages);
    }

    private ProjectMeterSummaryResponse buildFallbackSummary(
            ProjectEntity project,
            List<ProjectConstructionStageEntity> stages
    ) {

        int overallProgress = calculateOverallProgress(stages);
        ProjectTimelineResponse timeline = resolveTimeline(project, null);
        int delayDays = "DELAYED".equals(timeline.getTimelineStatus()) && timeline.getDelayVsLatestReraDays() != null
            ? Math.max(timeline.getDelayVsLatestReraDays(), 0)
            : 0;

        OffsetDateTime fallbackUpdatedAt = project.getUpdatedAt() != null
            ? project.getUpdatedAt()
            : project.getCreatedAt();

        return ProjectMeterSummaryResponse.builder()
            .projectId(project.getId())
            .constructionProgressPercent(overallProgress)
            .delayDays(delayDays)
            .constructionStartDate(project.getStartDate())
            .expectedCompletionDate(project.getPossessionDate())
            .revisedCompletionDate(null)
            .timeline(timeline)
            .verified(false)
            .computedAt(null)
            .lastVerifiedAt(null)
            .lastUpdatedAt(fallbackUpdatedAt)
            .build();
    }

    private int calculateOverallProgress(List<ProjectConstructionStageEntity> stages) {
        if (stages == null || stages.isEmpty()) {
            return 0;
        }

        int weightedSum = 0;
        int totalWeight = 0;

        for (ProjectConstructionStageEntity stage : stages) {
            int progress = safePercent(stage.getProgressPercent());
            int weight = stage.getWeightPercent() == null ? 0 : Math.max(stage.getWeightPercent(), 0);

            weightedSum += (progress * weight);
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return 0;
        }

        return Math.round((float) weightedSum / totalWeight);
    }

    private int calculateDelayDays(ProjectEntity project, ProjectMeterSnapshotEntity snapshot) {
        LocalDate baselineDate = null;

        if (snapshot != null && snapshot.getRevisedCompletionDate() != null) {
            baselineDate = snapshot.getRevisedCompletionDate();
        } else if (snapshot != null && snapshot.getExpectedCompletionDate() != null) {
            baselineDate = snapshot.getExpectedCompletionDate();
        } else if (project.getPossessionDate() != null) {
            baselineDate = project.getPossessionDate();
        }

        if (baselineDate == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        if (!today.isAfter(baselineDate)) {
            return 0;
        }

        return (int) ChronoUnit.DAYS.between(baselineDate, today);
    }

    private ProjectTimelineResponse resolveTimeline(ProjectEntity project, ProjectMeterSnapshotEntity snapshot) {
        if (snapshot != null) {
            return ProjectMeterMapper.toTimelineResponse(snapshot);
        }

        ProjectMeterMapper.TimelineComputed computed = ProjectMeterMapper.computeTimeline(
            project.getPossessionDate(),
            project.getPossessionDate(),
            null,
            0
        );

        return ProjectTimelineResponse.builder()
            .originalCompletionDate(project.getPossessionDate())
            .latestReraCompletionDate(project.getPossessionDate())
            .actualCompletionDate(null)
            .reraExtensionCount(0)
            .delayVsOriginalDays(computed.delayVsOriginalDays())
            .delayVsLatestReraDays(computed.delayVsLatestReraDays())
            .timelineStatus(computed.status())
            .timelineLabel(computed.label())
            .timelineHint(computed.hint())
            .build();
    }

    private ProjectComplianceItemResponse toComplianceItemResponse(ProjectComplianceItemEntity entity) {
        return ProjectComplianceItemResponse.builder()
            .id(entity.getId())
            .itemGroup(entity.getItemGroup())
            .itemKey(entity.getItemKey())
            .itemLabel(entity.getItemLabel())
            .status(entity.getStatus())
            .valueText(entity.getValueText())
            .documentUrl(entity.getDocumentUrl())
            .remarks(entity.getRemarks())
            .displayOrder(entity.getDisplayOrder())
            .verified(entity.getVerified())
            .build();
    }

    private ProjectPriceHistoryPointResponse toPriceHistoryPointResponse(ProjectPriceHistoryEntity entity) {
        return ProjectPriceHistoryPointResponse.builder()
            .id(entity.getId())
            .yearLabel(entity.getYearLabel())
            .projectPrice(entity.getProjectPrice())
            .averageAreaPrice(entity.getAverageAreaPrice())
            .displayOrder(entity.getDisplayOrder())
            .verified(entity.getVerified())
            .build();
    }

    private ProjectPaymentMilestoneResponse toPaymentMilestoneResponse(ProjectPaymentMilestoneEntity entity) {
        return ProjectPaymentMilestoneResponse.builder()
            .id(entity.getId())
            .milestoneCode(entity.getMilestoneCode())
            .milestoneLabel(entity.getMilestoneLabel())
            .description(entity.getDescription())
            .percentageValue(entity.getPercentageValue())
            .linkedStageCode(entity.getLinkedStageCode() != null ? entity.getLinkedStageCode().name() : null)
            .displayOrder(entity.getDisplayOrder())
            .active(entity.getActive())
            .build();
    }

    private ProjectCostBreakdownResponse toCostBreakdownResponse(ProjectCostBreakdownEntity entity) {
        return ProjectCostBreakdownResponse.builder()
            .totalCost(entity.getTotalCost())
            .landCost(entity.getLandCost())
            .constructionCost(entity.getConstructionCost())
            .infrastructureCost(entity.getInfrastructureCost())
            .otherCost(entity.getOtherCost())
            .sourceLabel(entity.getSourceLabel())
            .remarks(entity.getRemarks())
            .verified(entity.getVerified())
            .build();
    }

    private ProjectLandUtilizationResponse toLandUtilizationResponse(ProjectLandUtilizationEntity entity) {
        return ProjectLandUtilizationResponse.builder()
            .totalLandAreaSqm(entity.getTotalLandAreaSqm())
            .commercialAreaSqm(entity.getCommercialAreaSqm())
            .parksAreaSqm(entity.getParksAreaSqm())
            .openAreaSqm(entity.getOpenAreaSqm())
            .residentialAreaSqm(entity.getResidentialAreaSqm())
            .parkingAreaSqm(entity.getParkingAreaSqm())
            .utilityAreaSqm(entity.getUtilityAreaSqm())
            .build();
    }

    private ProjectLocationRadarResponse toLocationRadarResponse(ProjectLocationScoreEntity entity) {
        return ProjectLocationRadarResponse.builder()
            .metroScore(entity.getMetroScore())
            .educationScore(entity.getEducationScore())
            .healthcareScore(entity.getHealthcareScore())
            .retailScore(entity.getRetailScore())
            .jobScore(entity.getJobScore())
            .leisureScore(entity.getLeisureScore())
            .currentStrengthScore(entity.getCurrentStrengthScore())
            .futureGrowthScore(entity.getFutureGrowthScore())
            .finalScore(entity.getFinalScore())
            .appreciationPercent3Y(entity.getAppreciationPercent3Y())
            .scoreSummary(entity.getScoreSummary())
            .verified(entity.getVerified())
            .build();
    }

    private int safePercent(Integer value) {
        if (value == null) return 0;
        return Math.max(0, Math.min(100, value));
    }

    /**
     * Credibility is optional for dashboard preview. Check the already-loaded
     * builder before crossing the transactional credibility-service proxy: a
     * runtime exception escaping that proxy would mark the shared read
     * transaction rollback-only even if caught here afterwards.
     */
    private BuilderCredibilitySummaryResponse safeBuilderCredibility(ProjectEntity project) {
        if (!projectPublicVisibilityPolicy.isBuilderPubliclyAvailable(project.getBuilder())) {
            log.info(
                "No builder credibility available for projectId={}: builder is not public-ready",
                project.getId()
            );
            return null;
        }
        return builderCredibilityService.publicGetCredibilitySummaryForLoadedBuilder(project.getBuilder());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectMeterCardResponse> publicListMeterCards(Long cityId, Pageable pageable) {
        Page<ProjectEntity> page = (cityId == null)
            ? projectRepository.findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                ReviewStatus.APPROVED,
                pageable
            )
            : projectRepository.findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
                cityId,
                ReviewStatus.APPROVED,
                pageable
            );

        List<Long> projectIds = page.getContent().stream()
            .map(ProjectEntity::getId)
            .toList();

        java.util.Map<Long, ProjectMeterSnapshotEntity> snapshotMap = java.util.Collections.emptyMap();
        java.util.Map<Long, java.util.List<ProjectMediaEntity>> mediaMap = java.util.Collections.emptyMap();

        if (!projectIds.isEmpty()) {
            snapshotMap = projectMeterSnapshotRepository.findByProjectIdIn(projectIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                    s -> s.getProject().getId(),
                    s -> s
                ));

            var mediaList = projectMediaRepository.findActiveByProjectIds(projectIds);
            mediaMap = mediaList.stream()
                .collect(java.util.stream.Collectors.groupingBy(m -> m.getProject().getId()));
        }

        final var finalSnapshotMap = snapshotMap;
        final var finalMediaMap = mediaMap;

        List<ProjectMeterCardResponse> cards = page.getContent().stream()
            .map(project -> ProjectMeterMapper.toCardResponse(
                project,
                finalSnapshotMap.get(project.getId()),
                finalMediaMap.getOrDefault(project.getId(), java.util.List.of())
            ))
            .toList();

        projectFavoriteService.enrichProjectMeterCards(cards);

        return new org.springframework.data.domain.PageImpl<>(cards, pageable, page.getTotalElements());
    }
}
