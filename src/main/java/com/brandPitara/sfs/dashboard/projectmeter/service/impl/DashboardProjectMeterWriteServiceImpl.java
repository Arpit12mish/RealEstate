package com.brandPitara.sfs.dashboard.projectmeter.service.impl;

import com.brandPitara.sfs.dashboard.projectmeter.dto.*;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.dashboard.projectmeter.service.DashboardProjectMeterWriteService;
import com.brandPitara.sfs.dashboard.validator.DashboardProjectMeterValidator;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.dto.*;
import com.brandPitara.sfs.projectmeter.entity.*;
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterMapper;
import com.brandPitara.sfs.projectmeter.repository.*;
import java.time.OffsetDateTime;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterSnapshotRecalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardProjectMeterWriteServiceImpl implements DashboardProjectMeterWriteService {

    private final ProjectRepository projectRepository;
    private final ProjectMeterSnapshotRepository projectMeterSnapshotRepository;
    private final ProjectConstructionStageRepository stageRepository;
    private final ProjectComplianceItemRepository complianceRepository;
    private final ProjectAmenityProgressRepository amenityRepository;
    private final ProjectPriceHistoryRepository priceHistoryRepository;
    private final ProjectPaymentMilestoneRepository paymentMilestoneRepository;
    private final ProjectCostBreakdownRepository costBreakdownRepository;
    private final ProjectLandUtilizationRepository landUtilizationRepository;
    private final ProjectLocationScoreRepository locationScoreRepository;
    private final ProjectMeterSnapshotRecalculationService recalculationService;
    private final DashboardProjectMeterValidator meterValidator;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectConstructionStageResponse> listConstructionStages(Long projectId) {
        assertProjectExists(projectId);
        return stageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId)
                .stream()
                .map(ProjectMeterMapper::toStageResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectConstructionStageResponse createConstructionStage(Long projectId, DashboardProjectConstructionStageRequest request) {
        meterValidator.validateConstructionStage(request);
        ProjectEntity project = getProject(projectId);

        ProjectConstructionStageEntity entity = ProjectConstructionStageEntity.builder()
                .project(project)
                .stageCode(request.getStageCode())
                .stageLabel(cleanRequired(request.getStageLabel()))
                .displayOrder(request.getDisplayOrder())
                .weightPercent(safeNonNegative(request.getWeightPercent()))
                .progressPercent(safePercent(request.getProgressPercent()))
                .plannedStartDate(request.getPlannedStartDate())
                .plannedEndDate(request.getPlannedEndDate())
                .actualStartDate(request.getActualStartDate())
                .actualEndDate(request.getActualEndDate())
                .status(request.getStatus())
                .remarks(clean(request.getRemarks()))
                .evidenceCount(request.getEvidenceCount() != null ? Math.max(request.getEvidenceCount(), 0) : 0)
                .verified(Boolean.TRUE.equals(request.getVerified()))
                .build();

        return ProjectMeterMapper.toStageResponse(stageRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectConstructionStageResponse updateConstructionStage(Long projectId, Long stageId, DashboardProjectConstructionStageRequest request) {
        meterValidator.validateConstructionStage(request);
        ProjectConstructionStageEntity entity = stageRepository.findByIdAndProjectId(stageId, projectId)
                .orElseThrow(() -> new NotFoundException("Construction stage not found: " + stageId));

        entity.setStageCode(request.getStageCode());
        entity.setStageLabel(cleanRequired(request.getStageLabel()));
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setWeightPercent(safeNonNegative(request.getWeightPercent()));
        entity.setProgressPercent(safePercent(request.getProgressPercent()));
        entity.setPlannedStartDate(request.getPlannedStartDate());
        entity.setPlannedEndDate(request.getPlannedEndDate());
        entity.setActualStartDate(request.getActualStartDate());
        entity.setActualEndDate(request.getActualEndDate());
        entity.setStatus(request.getStatus());
        entity.setRemarks(clean(request.getRemarks()));
        entity.setEvidenceCount(request.getEvidenceCount() != null ? Math.max(request.getEvidenceCount(), 0) : 0);
        entity.setVerified(Boolean.TRUE.equals(request.getVerified()));

        return ProjectMeterMapper.toStageResponse(stageRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteConstructionStage(Long projectId, Long stageId) {
        ProjectConstructionStageEntity entity = stageRepository.findByIdAndProjectId(stageId, projectId)
                .orElseThrow(() -> new NotFoundException("Construction stage not found: " + stageId));
        stageRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectComplianceItemResponse> listComplianceItems(Long projectId) {
        assertProjectExists(projectId);
        return complianceRepository.findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(projectId)
                .stream()
                .map(this::toComplianceItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectComplianceItemResponse createComplianceItem(Long projectId, DashboardProjectComplianceItemRequest request) {
        ProjectEntity project = getProject(projectId);

        ProjectComplianceItemEntity entity = ProjectComplianceItemEntity.builder()
                .project(project)
                .itemGroup(request.getItemGroup())
                .itemKey(cleanRequired(request.getItemKey()))
                .itemLabel(cleanRequired(request.getItemLabel()))
                .status(request.getStatus())
                .valueText(clean(request.getValueText()))
                .documentUrl(clean(request.getDocumentUrl()))
                .remarks(clean(request.getRemarks()))
                .displayOrder(request.getDisplayOrder())
                .verified(Boolean.TRUE.equals(request.getVerified()))
                .build();

        return toComplianceItemResponse(complianceRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectComplianceItemResponse updateComplianceItem(Long projectId, Long itemId, DashboardProjectComplianceItemRequest request) {
        ProjectComplianceItemEntity entity = complianceRepository.findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Compliance item not found: " + itemId));

        entity.setItemGroup(request.getItemGroup());
        entity.setItemKey(cleanRequired(request.getItemKey()));
        entity.setItemLabel(cleanRequired(request.getItemLabel()));
        entity.setStatus(request.getStatus());
        entity.setValueText(clean(request.getValueText()));
        entity.setDocumentUrl(clean(request.getDocumentUrl()));
        entity.setRemarks(clean(request.getRemarks()));
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setVerified(Boolean.TRUE.equals(request.getVerified()));

        return toComplianceItemResponse(complianceRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteComplianceItem(Long projectId, Long itemId) {
        ProjectComplianceItemEntity entity = complianceRepository.findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Compliance item not found: " + itemId));
        complianceRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectAmenityItemResponse> listAmenities(Long projectId) {
        assertProjectExists(projectId);
        return amenityRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId)
                .stream()
                .map(this::toAmenityResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectAmenityItemResponse createAmenity(Long projectId, DashboardProjectAmenityRequest request) {
        ProjectEntity project = getProject(projectId);

        ProjectAmenityProgressEntity entity = ProjectAmenityProgressEntity.builder()
                .project(project)
                .amenityCode(cleanRequired(request.getAmenityCode()))
                .amenityLabel(cleanRequired(request.getAmenityLabel()))
                .status(request.getStatus())
                .progressPercent(safePercent(request.getProgressPercent()))
                .weightPercent(safeNonNegative(request.getWeightPercent()))
                .displayOrder(request.getDisplayOrder())
                .remarks(clean(request.getRemarks()))
                .verified(Boolean.TRUE.equals(request.getVerified()))
                .category(request.getCategory())
                .categoryLabel(clean(request.getCategoryLabel()))
                .iconKey(clean(request.getIconKey()))
                .rare(Boolean.TRUE.equals(request.getRare()))
                .available(request.getAvailable() == null ? true : request.getAvailable())
                .publicVisible(request.getPublicVisible() == null ? true : request.getPublicVisible())
                .active(request.getActive() == null ? true : request.getActive())
                .categoryDisplayOrder(request.getCategoryDisplayOrder() != null ? request.getCategoryDisplayOrder() : 0)
                .build();

        return toAmenityResponse(amenityRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectAmenityItemResponse updateAmenity(Long projectId, Long amenityId, DashboardProjectAmenityRequest request) {
        ProjectAmenityProgressEntity entity = amenityRepository.findByIdAndProjectId(amenityId, projectId)
                .orElseThrow(() -> new NotFoundException("Amenity item not found: " + amenityId));

        entity.setAmenityCode(cleanRequired(request.getAmenityCode()));
        entity.setAmenityLabel(cleanRequired(request.getAmenityLabel()));
        entity.setStatus(request.getStatus());
        entity.setProgressPercent(safePercent(request.getProgressPercent()));
        entity.setWeightPercent(safeNonNegative(request.getWeightPercent()));
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setRemarks(clean(request.getRemarks()));
        entity.setVerified(Boolean.TRUE.equals(request.getVerified()));
        if (request.getCategory() != null) entity.setCategory(request.getCategory());
        if (request.getCategoryLabel() != null) entity.setCategoryLabel(clean(request.getCategoryLabel()));
        if (request.getIconKey() != null) entity.setIconKey(clean(request.getIconKey()));
        if (request.getRare() != null) entity.setRare(request.getRare());
        if (request.getAvailable() != null) entity.setAvailable(request.getAvailable());
        if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
        if (request.getActive() != null) entity.setActive(request.getActive());
        if (request.getCategoryDisplayOrder() != null) entity.setCategoryDisplayOrder(request.getCategoryDisplayOrder());

        return toAmenityResponse(amenityRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteAmenity(Long projectId, Long amenityId) {
        ProjectAmenityProgressEntity entity = amenityRepository.findByIdAndProjectId(amenityId, projectId)
                .orElseThrow(() -> new NotFoundException("Amenity item not found: " + amenityId));
        amenityRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectPriceHistoryPointResponse> listPriceHistory(Long projectId) {
        assertProjectExists(projectId);
        return priceHistoryRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId)
                .stream()
                .map(this::toPriceHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectPriceHistoryPointResponse createPriceHistory(Long projectId, DashboardProjectPriceHistoryRequest request) {
        ProjectEntity project = getProject(projectId);

        ProjectPriceHistoryEntity entity = ProjectPriceHistoryEntity.builder()
                .project(project)
                .yearLabel(cleanRequired(request.getYearLabel()))
                .projectPrice(request.getProjectPrice())
                .averageAreaPrice(request.getAverageAreaPrice())
                .displayOrder(request.getDisplayOrder())
                .verified(Boolean.TRUE.equals(request.getVerified()))
                .build();

        return toPriceHistoryResponse(priceHistoryRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectPriceHistoryPointResponse updatePriceHistory(Long projectId, Long priceHistoryId, DashboardProjectPriceHistoryRequest request) {
        ProjectPriceHistoryEntity entity = priceHistoryRepository.findByIdAndProjectId(priceHistoryId, projectId)
                .orElseThrow(() -> new NotFoundException("Price history item not found: " + priceHistoryId));

        entity.setYearLabel(cleanRequired(request.getYearLabel()));
        entity.setProjectPrice(request.getProjectPrice());
        entity.setAverageAreaPrice(request.getAverageAreaPrice());
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setVerified(Boolean.TRUE.equals(request.getVerified()));

        return toPriceHistoryResponse(priceHistoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void deletePriceHistory(Long projectId, Long priceHistoryId) {
        ProjectPriceHistoryEntity entity = priceHistoryRepository.findByIdAndProjectId(priceHistoryId, projectId)
                .orElseThrow(() -> new NotFoundException("Price history item not found: " + priceHistoryId));
        priceHistoryRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectPaymentMilestoneResponse> listPaymentMilestones(Long projectId) {
        assertProjectExists(projectId);
        return paymentMilestoneRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId)
                .stream()
                .map(this::toPaymentMilestoneResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectPaymentMilestoneResponse createPaymentMilestone(Long projectId, DashboardProjectPaymentMilestoneRequest request) {
        ProjectEntity project = getProject(projectId);

        ProjectPaymentMilestoneEntity entity = ProjectPaymentMilestoneEntity.builder()
                .project(project)
                .milestoneCode(cleanRequired(request.getMilestoneCode()))
                .milestoneLabel(cleanRequired(request.getMilestoneLabel()))
                .description(clean(request.getDescription()))
                .percentageValue(request.getPercentageValue())
                .displayOrder(request.getDisplayOrder())
                .linkedStageCode(request.getLinkedStageCode())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return toPaymentMilestoneResponse(paymentMilestoneRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectPaymentMilestoneResponse updatePaymentMilestone(Long projectId, Long milestoneId, DashboardProjectPaymentMilestoneRequest request) {
        ProjectPaymentMilestoneEntity entity = paymentMilestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new NotFoundException("Payment milestone not found: " + milestoneId));

        entity.setMilestoneCode(cleanRequired(request.getMilestoneCode()));
        entity.setMilestoneLabel(cleanRequired(request.getMilestoneLabel()));
        entity.setDescription(clean(request.getDescription()));
        entity.setPercentageValue(request.getPercentageValue());
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setLinkedStageCode(request.getLinkedStageCode());
        entity.setActive(request.getActive() != null ? request.getActive() : true);

        return toPaymentMilestoneResponse(paymentMilestoneRepository.save(entity));
    }

    @Override
    @Transactional
    public void deletePaymentMilestone(Long projectId, Long milestoneId) {
        ProjectPaymentMilestoneEntity entity = paymentMilestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new NotFoundException("Payment milestone not found: " + milestoneId));
        paymentMilestoneRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectCostBreakdownResponse getCostBreakdown(Long projectId) {
        assertProjectExists(projectId);
        return costBreakdownRepository.findByProjectId(projectId)
                .map(this::toCostBreakdownResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public ProjectCostBreakdownResponse upsertCostBreakdown(Long projectId, DashboardProjectCostBreakdownRequest request) {
        meterValidator.validateCostBreakdown(request);
        ProjectEntity project = getProject(projectId);

        ProjectCostBreakdownEntity entity = costBreakdownRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectCostBreakdownEntity.builder().project(project).build());

        entity.setLandCost(request.getLandCost());
        entity.setConstructionCost(request.getConstructionCost());
        entity.setInfrastructureCost(request.getInfrastructureCost());
        entity.setOtherCost(request.getOtherCost());
        entity.setTotalCost(resolveTotalCost(request));
        entity.setSourceLabel(clean(request.getSourceLabel()));
        entity.setRemarks(clean(request.getRemarks()));
        entity.setVerified(Boolean.TRUE.equals(request.getVerified()));

        return toCostBreakdownResponse(costBreakdownRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectLandUtilizationResponse getLandUtilization(Long projectId) {
        assertProjectExists(projectId);
        return landUtilizationRepository.findByProjectId(projectId)
                .map(this::toLandUtilizationResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public ProjectLandUtilizationResponse upsertLandUtilization(Long projectId, DashboardProjectLandUtilizationRequest request) {
        meterValidator.validateLandUtilization(request);
        ProjectEntity project = getProject(projectId);

        ProjectLandUtilizationEntity entity = landUtilizationRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectLandUtilizationEntity.builder().project(project).build());

        entity.setTotalLandAreaSqm(request.getTotalLandAreaSqm());
        entity.setCommercialAreaSqm(request.getCommercialAreaSqm());
        entity.setParksAreaSqm(request.getParksAreaSqm());
        entity.setOpenAreaSqm(request.getOpenAreaSqm());
        entity.setResidentialAreaSqm(request.getResidentialAreaSqm());
        entity.setParkingAreaSqm(request.getParkingAreaSqm());
        entity.setUtilityAreaSqm(request.getUtilityAreaSqm());

        return toLandUtilizationResponse(landUtilizationRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectLocationRadarResponse getLocationScore(Long projectId) {
        assertProjectExists(projectId);
        return locationScoreRepository.findByProjectId(projectId)
                .map(this::toLocationRadarResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public ProjectLocationRadarResponse upsertLocationScore(Long projectId, DashboardProjectLocationScoreRequest request) {
        ProjectEntity project = getProject(projectId);

        ProjectLocationScoreEntity entity = locationScoreRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectLocationScoreEntity.builder().project(project).build());

        entity.setMetroScore(request.getMetroScore());
        entity.setEducationScore(request.getEducationScore());
        entity.setHealthcareScore(request.getHealthcareScore());
        entity.setRetailScore(request.getRetailScore());
        entity.setJobScore(request.getJobScore());
        entity.setLeisureScore(request.getLeisureScore());
        entity.setCurrentStrengthScore(request.getCurrentStrengthScore());
        entity.setFutureGrowthScore(request.getFutureGrowthScore());
        entity.setFinalScore(request.getFinalScore());
        entity.setAppreciationPercent3Y(request.getAppreciationPercent3Y());
        entity.setScoreSummary(clean(request.getScoreSummary()));
        entity.setVerified(Boolean.TRUE.equals(request.getVerified()));

        return toLocationRadarResponse(locationScoreRepository.save(entity));
    }

    @Override
    @Transactional
    public DashboardProjectMeterWriteResponse recalculateSnapshot(Long projectId) {
        assertProjectExists(projectId);
        recalculationService.recalculateSnapshot(projectId);

        return DashboardProjectMeterWriteResponse.builder()
                .projectId(projectId)
                .section("SNAPSHOT")
                .message("Project meter snapshot recalculated successfully")
                .build();
    }

    @Override
    @Transactional
    public DashboardProjectMeterWriteResponse verifySnapshot(Long projectId, DashboardProjectMeterSnapshotVerifyRequest request) {
        assertProjectExists(projectId);
        com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity snapshot =
                projectMeterSnapshotRepository.findByProjectId(projectId)
                        .orElseThrow(() -> new NotFoundException(
                                "Project meter snapshot not found for project " + projectId
                                + ". Recalculate snapshot first."));

        boolean verified = Boolean.TRUE.equals(request.getVerified());
        snapshot.setVerified(verified);
        snapshot.setLastVerifiedAt(verified ? OffsetDateTime.now() : null);
        projectMeterSnapshotRepository.save(snapshot);

        String msg = verified
                ? "Project meter snapshot marked as verified"
                : "Project meter snapshot verification cleared";
        return DashboardProjectMeterWriteResponse.builder()
                .projectId(projectId)
                .section("SNAPSHOT")
                .message(msg)
                .build();
    }

    @Override
    @Transactional
    public DashboardProjectMeterWriteResponse updateTimeline(Long projectId, DashboardProjectTimelineRequest request) {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        ProjectMeterSnapshotEntity snapshot = projectMeterSnapshotRepository.findByProjectId(projectId)
                .orElseGet(() -> ProjectMeterSnapshotEntity.builder()
                        .project(project)
                        .constructionStartDate(project.getStartDate())
                        .expectedCompletionDate(project.getPossessionDate())
                        .originalCompletionDate(project.getPossessionDate())
                        .latestReraCompletionDate(project.getPossessionDate())
                        .build());

        snapshot.setOriginalCompletionDate(request.getOriginalCompletionDate());
        snapshot.setLatestReraCompletionDate(request.getLatestReraCompletionDate());
        snapshot.setActualCompletionDate(request.getActualCompletionDate());
        snapshot.setReraExtensionCount(request.getReraExtensionCount() != null ? request.getReraExtensionCount() : 0);

        if (snapshot.getExpectedCompletionDate() == null) {
            snapshot.setExpectedCompletionDate(request.getOriginalCompletionDate());
        }
        snapshot.setRevisedCompletionDate(request.getLatestReraCompletionDate());

        ProjectMeterMapper.TimelineComputed timeline = ProjectMeterMapper.computeTimeline(
                snapshot.getOriginalCompletionDate(),
                snapshot.getLatestReraCompletionDate(),
                snapshot.getActualCompletionDate(),
                snapshot.getReraExtensionCount()
        );

        snapshot.setDelayVsOriginalDays(timeline.delayVsOriginalDays());
        snapshot.setDelayVsLatestReraDays(timeline.delayVsLatestReraDays());
        snapshot.setTimelineStatus(timeline.status());
        snapshot.setTimelineLabel(timeline.label());
        snapshot.setTimelineHint(timeline.hint());
        snapshot.setDelayDays("DELAYED".equals(timeline.status()) ? Math.max(timeline.delayVsLatestReraDays(), 0) : 0);
        snapshot.setComputedAt(OffsetDateTime.now());

        projectMeterSnapshotRepository.save(snapshot);

        return DashboardProjectMeterWriteResponse.builder()
                .projectId(projectId)
                .section("TIMELINE")
                .message("Project RERA timeline saved")
                .build();
    }

    @Override
    @Transactional
    public DashboardProjectMeterWriteResponse updatePriceInsights(Long projectId, DashboardProjectPriceInsightsRequest request) {
        assertProjectExists(projectId);
        ProjectMeterSnapshotEntity snapshot = projectMeterSnapshotRepository.findByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException(
                        "Project meter snapshot not found for project " + projectId
                        + ". Recalculate snapshot first."));

        if (request.getLaunchPrice() != null) {
            snapshot.setLaunchPrice(request.getLaunchPrice());
        }
        if (request.getCurrentPrice() != null) {
            snapshot.setCurrentPrice(request.getCurrentPrice());
        }
        if (request.getAverageAreaPrice() != null) {
            snapshot.setAverageAreaPrice(request.getAverageAreaPrice());
        }

        Long launchPrice = snapshot.getLaunchPrice();
        Long currentPrice = snapshot.getCurrentPrice();
        snapshot.setPriceAppreciationPercent(calculatePriceAppreciationPercent(launchPrice, currentPrice));

        projectMeterSnapshotRepository.save(snapshot);

        return DashboardProjectMeterWriteResponse.builder()
                .projectId(projectId)
                .section("PRICE_INSIGHTS")
                .message("Price insights updated successfully")
                .build();
    }

    private Double calculatePriceAppreciationPercent(Long launchPrice, Long currentPrice) {
        if (launchPrice == null || currentPrice == null || launchPrice <= 0) {
            return null;
        }
        java.math.BigDecimal launch = java.math.BigDecimal.valueOf(launchPrice);
        java.math.BigDecimal current = java.math.BigDecimal.valueOf(currentPrice);
        return current.subtract(launch)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(launch, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    private ProjectEntity getProject(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    private void assertProjectExists(Long projectId) {
        getProject(projectId);
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

    private ProjectAmenityItemResponse toAmenityResponse(ProjectAmenityProgressEntity entity) {
        ProjectAmenityCategory cat = entity.getCategory();
        String catLabel = entity.getCategoryLabel();
        if (catLabel == null || catLabel.isBlank()) {
            catLabel = cat != null ? cat.toLabel() : ProjectAmenityCategory.OTHER.toLabel();
        }
        return ProjectAmenityItemResponse.builder()
                .id(entity.getId())
                .amenityCode(entity.getAmenityCode())
                .amenityLabel(entity.getAmenityLabel())
                .category(cat)
                .categoryLabel(catLabel)
                .iconKey(entity.getIconKey())
                .rare(entity.getRare())
                .available(entity.getAvailable())
                .status(entity.getStatus())
                .progressPercent(entity.getProgressPercent())
                .weightPercent(entity.getWeightPercent())
                .displayOrder(entity.getDisplayOrder())
                .categoryDisplayOrder(entity.getCategoryDisplayOrder())
                .remarks(entity.getRemarks())
                .verified(entity.getVerified())
                .publicVisible(entity.getPublicVisible())
                .active(entity.getActive())
                .build();
    }

    private ProjectPriceHistoryPointResponse toPriceHistoryResponse(ProjectPriceHistoryEntity entity) {
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

    private Long resolveTotalCost(DashboardProjectCostBreakdownRequest request) {
        if (request.getTotalCost() != null) {
            return request.getTotalCost();
        }

        long total = 0L;
        boolean hasAny = false;

        if (request.getLandCost() != null) {
            total += request.getLandCost();
            hasAny = true;
        }
        if (request.getConstructionCost() != null) {
            total += request.getConstructionCost();
            hasAny = true;
        }
        if (request.getInfrastructureCost() != null) {
            total += request.getInfrastructureCost();
            hasAny = true;
        }
        if (request.getOtherCost() != null) {
            total += request.getOtherCost();
            hasAny = true;
        }

        return hasAny ? total : null;
    }

    private int safePercent(Integer value) {
        if (value == null) return 0;
        return Math.max(0, Math.min(100, value));
    }

    private int safeNonNegative(Integer value) {
        if (value == null) return 0;
        return Math.max(value, 0);
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String cleanRequired(String value) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException("Required value is missing");
        }
        return cleaned;
    }
}
