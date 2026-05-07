package com.brandPitara.sfs.dashboard.projectmeter.service.impl;

import com.brandPitara.sfs.dashboard.projectmeter.dto.*;
import com.brandPitara.sfs.dashboard.projectmeter.service.DashboardProjectMeterWriteService;
import com.brandPitara.sfs.dashboard.validator.DashboardProjectMeterValidator;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.dto.*;
import com.brandPitara.sfs.projectmeter.entity.*;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterMapper;
import com.brandPitara.sfs.projectmeter.repository.*;
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
                .itemKey(entity.getItemKey())
                .itemLabel(entity.getItemLabel())
                .status(entity.getStatus())
                .valueText(entity.getValueText())
                .documentUrl(entity.getDocumentUrl())
                .remarks(entity.getRemarks())
                .verified(entity.getVerified())
                .build();
    }

    private ProjectAmenityItemResponse toAmenityResponse(ProjectAmenityProgressEntity entity) {
        return ProjectAmenityItemResponse.builder()
                .id(entity.getId())
                .amenityCode(entity.getAmenityCode())
                .amenityLabel(entity.getAmenityLabel())
                .status(entity.getStatus())
                .progressPercent(entity.getProgressPercent())
                .weightPercent(entity.getWeightPercent())
                .remarks(entity.getRemarks())
                .verified(entity.getVerified())
                .build();
    }

    private ProjectPriceHistoryPointResponse toPriceHistoryResponse(ProjectPriceHistoryEntity entity) {
        return ProjectPriceHistoryPointResponse.builder()
                .yearLabel(entity.getYearLabel())
                .projectPrice(entity.getProjectPrice())
                .averageAreaPrice(entity.getAverageAreaPrice())
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