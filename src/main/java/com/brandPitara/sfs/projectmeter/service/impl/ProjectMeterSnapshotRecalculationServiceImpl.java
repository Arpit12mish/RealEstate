package com.brandPitara.sfs.projectmeter.service.impl;

import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectCostBreakdownEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectLocationScoreEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectCostBreakdownRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLocationScoreRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterSnapshotRecalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMeterSnapshotRecalculationServiceImpl implements ProjectMeterSnapshotRecalculationService {

    private final ProjectRepository projectRepository;
    private final ProjectMeterSnapshotRepository projectMeterSnapshotRepository;
    private final ProjectConstructionStageRepository projectConstructionStageRepository;
    private final ProjectComplianceItemRepository projectComplianceItemRepository;
    private final ProjectAmenityProgressRepository projectAmenityProgressRepository;
    private final ProjectLocationScoreRepository projectLocationScoreRepository;
    private final ProjectCostBreakdownRepository projectCostBreakdownRepository;

    @Override
    @Transactional
    public void recalculateSnapshot(Long projectId) {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        List<ProjectConstructionStageEntity> stages =
            projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        List<ProjectAmenityProgressEntity> amenities =
            projectAmenityProgressRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        List<ProjectComplianceItemEntity> complianceItems =
            projectComplianceItemRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        ProjectLocationScoreEntity locationScore =
            projectLocationScoreRepository.findByProjectId(projectId).orElse(null);

        ProjectCostBreakdownEntity costBreakdown =
            projectCostBreakdownRepository.findByProjectId(projectId).orElse(null);

        ProjectMeterSnapshotEntity snapshot = projectMeterSnapshotRepository.findByProjectId(projectId)
            .orElseGet(() -> ProjectMeterSnapshotEntity.builder()
                .project(project)
                .build());

        Integer constructionProgressPercent = calculateOverallProgress(stages);
        Integer delayDays = calculateDelayDays(project, snapshot);
        Integer complianceScore = calculateComplianceScore(complianceItems);
        Integer amenityScore = calculateAmenityCompletionPercent(amenities);

        Long launchPrice = snapshot.getLaunchPrice();
        Long currentPrice = snapshot.getCurrentPrice();
        Long averageAreaPrice = snapshot.getAverageAreaPrice();

        Double priceAppreciationPercent = calculatePriceAppreciationPercent(launchPrice, currentPrice);

        snapshot.setConstructionProgressPercent(constructionProgressPercent);
        snapshot.setDelayDays(delayDays);
        snapshot.setComplianceScore(complianceScore);
        snapshot.setAmenityScore(amenityScore);

        if (locationScore != null) {
            snapshot.setLocationScore(locationScore.getFinalScore());
            snapshot.setLocationAppreciationPercent3Y(locationScore.getAppreciationPercent3Y());
        }

        snapshot.setPriceAppreciationPercent(priceAppreciationPercent);

        if (costBreakdown != null) {
            snapshot.setEstimatedCostTotal(costBreakdown.getTotalCost());
        }

        if (snapshot.getExpectedCompletionDate() == null) {
            snapshot.setExpectedCompletionDate(project.getPossessionDate());
        }

        if (snapshot.getComputedAt() == null) {
            snapshot.setComputedAt(OffsetDateTime.now());
        } else {
            snapshot.setComputedAt(OffsetDateTime.now());
        }

        snapshot.setLastVerifiedAt(Boolean.TRUE.equals(snapshot.getVerified()) ? OffsetDateTime.now() : snapshot.getLastVerifiedAt());

        projectMeterSnapshotRepository.save(snapshot);
    }

    @Override
    @Transactional
    public void recalculateAllPublishedSnapshots() {
        int page = 0;
        while (true) {
            var projectPage = projectRepository.findByPublishedTrueAndActiveTrueAndDeletedFalse(
                PageRequest.of(page, 100)
            );

            if (projectPage.isEmpty()) {
                break;
            }

            for (ProjectEntity project : projectPage.getContent()) {
                recalculateSnapshot(project.getId());
            }

            if (!projectPage.hasNext()) {
                break;
            }
            page++;
        }
    }

    private Integer calculateOverallProgress(List<ProjectConstructionStageEntity> stages) {
        if (stages == null || stages.isEmpty()) {
            return 0;
        }

        int weightedSum = 0;
        int totalWeight = 0;

        for (ProjectConstructionStageEntity stage : stages) {
            int progress = safePercent(stage.getProgressPercent());
            int weight = stage.getWeightPercent() == null ? 0 : Math.max(stage.getWeightPercent(), 0);

            weightedSum += progress * weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return 0;
        }

        return Math.round((float) weightedSum / totalWeight);
    }

    private Integer calculateAmenityCompletionPercent(List<ProjectAmenityProgressEntity> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return 0;
        }

        int weightedSum = 0;
        int totalWeight = 0;

        for (ProjectAmenityProgressEntity amenity : amenities) {
            int progress = safePercent(amenity.getProgressPercent());
            int weight = amenity.getWeightPercent() == null ? 0 : Math.max(amenity.getWeightPercent(), 0);

            weightedSum += progress * weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return 0;
        }

        return Math.round((float) weightedSum / totalWeight);
    }

    private Integer calculateComplianceScore(List<ProjectComplianceItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int total = items.size();
        int approved = 0;
        int partial = 0;

        for (ProjectComplianceItemEntity item : items) {
            if (item.getStatus() == ProjectComplianceStatus.APPROVED) {
                approved++;
            } else if (item.getStatus() == ProjectComplianceStatus.PENDING) {
                partial++;
            }
        }

        double rawScore = ((approved * 1.0) + (partial * 0.5)) / total * 100.0;
        return (int) Math.round(rawScore);
    }

    private Integer calculateDelayDays(ProjectEntity project, ProjectMeterSnapshotEntity snapshot) {
        LocalDate baselineDate = null;

        if (snapshot.getRevisedCompletionDate() != null) {
            baselineDate = snapshot.getRevisedCompletionDate();
        } else if (snapshot.getExpectedCompletionDate() != null) {
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

    private Double calculatePriceAppreciationPercent(Long launchPrice, Long currentPrice) {
        if (launchPrice == null || currentPrice == null || launchPrice <= 0) {
            return null;
        }

        BigDecimal launch = BigDecimal.valueOf(launchPrice);
        BigDecimal current = BigDecimal.valueOf(currentPrice);

        return current.subtract(launch)
            .multiply(BigDecimal.valueOf(100))
            .divide(launch, 2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private int safePercent(Integer value) {
        if (value == null) return 0;
        return Math.max(0, Math.min(100, value));
    }
}