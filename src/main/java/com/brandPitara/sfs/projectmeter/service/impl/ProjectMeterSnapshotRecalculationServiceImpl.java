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
import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.brandPitara.sfs.projectmeter.entity.ProjectPriceHistoryEntity;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterMapper;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectCostBreakdownRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLocationScoreRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectPriceHistoryRepository;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterSnapshotRecalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
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
    private final ProjectPriceHistoryRepository projectPriceHistoryRepository;

    @Override
    @Transactional
    public void recalculateSnapshot(Long projectId) {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        List<ProjectConstructionStageEntity> stages =
            projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        List<ProjectAmenityProgressEntity> amenities =
            projectAmenityProgressRepository
                .findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(projectId);

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

        if (snapshot.getConstructionStartDate() == null && project.getStartDate() != null) {
            snapshot.setConstructionStartDate(project.getStartDate());
        }

        if (snapshot.getExpectedCompletionDate() == null) {
            snapshot.setExpectedCompletionDate(project.getPossessionDate());
        }
        if (snapshot.getOriginalCompletionDate() == null) {
            snapshot.setOriginalCompletionDate(snapshot.getExpectedCompletionDate());
        }
        if (snapshot.getLatestReraCompletionDate() == null) {
            snapshot.setLatestReraCompletionDate(
                snapshot.getRevisedCompletionDate() != null
                    ? snapshot.getRevisedCompletionDate()
                    : snapshot.getExpectedCompletionDate()
            );
        }

        Integer constructionProgressPercent = calculateOverallProgress(stages);
        applyTimeline(project, snapshot);
        Integer delayDays = snapshot.getDelayDays();
        Integer complianceScore = calculateComplianceScore(complianceItems);
        Integer amenityScore = calculateAmenityCompletionPercent(amenities);

        List<ProjectPriceHistoryEntity> priceHistory =
            projectPriceHistoryRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(projectId);

        Long launchPrice;
        Long currentPrice;
        Long averageAreaPrice;

        if (!priceHistory.isEmpty()) {
            launchPrice = priceHistory.get(0).getProjectPrice();
            currentPrice = priceHistory.get(priceHistory.size() - 1).getProjectPrice();

            Long derivedAvgAreaPrice = null;
            for (int i = priceHistory.size() - 1; i >= 0; i--) {
                if (priceHistory.get(i).getAverageAreaPrice() != null) {
                    derivedAvgAreaPrice = priceHistory.get(i).getAverageAreaPrice();
                    break;
                }
            }
            averageAreaPrice = derivedAvgAreaPrice != null
                ? derivedAvgAreaPrice
                : (project.getAveragePricePerSqft() != null
                    ? project.getAveragePricePerSqft()
                    : snapshot.getAverageAreaPrice());
        } else {
            launchPrice = snapshot.getLaunchPrice();
            currentPrice = snapshot.getCurrentPrice();
            averageAreaPrice = snapshot.getAverageAreaPrice();
        }

        Double priceAppreciationPercent = calculatePriceAppreciationPercent(launchPrice, currentPrice);

        snapshot.setConstructionProgressPercent(constructionProgressPercent);
        snapshot.setDelayDays(delayDays);
        snapshot.setComplianceScore(complianceScore);
        snapshot.setAmenityScore(amenityScore);

        snapshot.setLaunchPrice(launchPrice);
        snapshot.setCurrentPrice(currentPrice);
        snapshot.setAverageAreaPrice(averageAreaPrice);

        if (locationScore != null) {
            snapshot.setLocationScore(locationScore.getFinalScore());
            snapshot.setLocationAppreciationPercent3Y(locationScore.getAppreciationPercent3Y());
        }

        snapshot.setPriceAppreciationPercent(priceAppreciationPercent);

        if (costBreakdown != null) {
            snapshot.setEstimatedCostTotal(costBreakdown.getTotalCost());
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
        int simpleSum = 0;
        int simpleCount = 0;

        for (ProjectAmenityProgressEntity amenity : amenities) {
            if (Boolean.FALSE.equals(amenity.getAvailable())) continue;
            if (amenity.getStatus() == ProjectAmenityStatus.NOT_AVAILABLE) continue;

            int progress = safePercent(amenity.getProgressPercent());
            int weight = amenity.getWeightPercent() == null ? 0 : Math.max(amenity.getWeightPercent(), 0);

            weightedSum += progress * weight;
            totalWeight += weight;
            simpleSum += progress;
            simpleCount++;
        }

        if (totalWeight > 0) {
            return Math.round((float) weightedSum / totalWeight);
        }
        if (simpleCount > 0) {
            return Math.round((float) simpleSum / simpleCount);
        }
        return 0;
    }

    private Integer calculateComplianceScore(List<ProjectComplianceItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int total = 0;
        int approved = 0;
        int partial = 0;

        for (ProjectComplianceItemEntity item : items) {
            if (item.getStatus() == ProjectComplianceStatus.NOT_APPLICABLE) {
                continue;
            }
            total++;
            if (item.getStatus() == ProjectComplianceStatus.OBTAINED) {
                approved++;
            } else if (item.getStatus() == ProjectComplianceStatus.PENDING
                    || item.getStatus() == ProjectComplianceStatus.EXPIRED) {
                partial++;
            }
        }

        if (total == 0) {
            return 0;
        }

        double rawScore = ((approved * 1.0) + (partial * 0.5)) / total * 100.0;
        return (int) Math.round(rawScore);
    }

    private void applyTimeline(ProjectEntity project, ProjectMeterSnapshotEntity snapshot) {
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
