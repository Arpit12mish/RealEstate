package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.FloorPlanInsightResponse;
import com.brandPitara.sfs.project.dto.FloorPlanInsightUpsertRequest;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanInsightDetailResponse;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanInsightEntity;
import com.brandPitara.sfs.project.mapper.ProjectFloorPlanInsightMapper;
import com.brandPitara.sfs.project.mapper.ProjectFloorPlanRoomDimensionMapper;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanInsightRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRoomDimensionRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFloorPlanInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFloorPlanInsightServiceImpl implements ProjectFloorPlanInsightService {

  private final ProjectRepository projectRepository;
  private final ProjectFloorPlanRepository floorPlanRepository;
  private final ProjectFloorPlanInsightRepository insightRepository;
  private final ProjectFloorPlanRoomDimensionRepository roomRepository;
  private final ContentVersionService contentVersionService;
  private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

  @Override
  @Transactional
  public FloorPlanInsightResponse create(Long projectId, Long floorPlanId, FloorPlanInsightUpsertRequest request) {
    ProjectFloorPlanEntity floorPlan = resolveFloorPlan(projectId, floorPlanId);

    ProjectFloorPlanInsightEntity entity = ProjectFloorPlanInsightEntity.builder()
        .floorPlan(floorPlan)
        .insightType(request.getInsightType())
        .title(request.getTitle().trim())
        .summary(clean(request.getSummary()))
        .detailedText(clean(request.getDetailedText()))
        .unitValue(request.getUnitValue())
        .benchmarkValue(request.getBenchmarkValue())
        .unitLabel(clean(request.getUnitLabel()))
        .differenceValue(request.getDifferenceValue())
        .differencePercent(request.getDifferencePercent())
        .chartLabelThisUnit(clean(request.getChartLabelThisUnit()))
        .chartLabelAverage(clean(request.getChartLabelAverage()))
        .comparisonResult(request.getComparisonResult())
        .score(request.getScore())
        .positive(request.getPositive() != null ? request.getPositive() : true)
        .publicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : true)
        .verified(request.getVerified() != null ? request.getVerified() : false)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .build();

    ProjectFloorPlanInsightEntity saved = insightRepository.save(entity);

    recalculateInsightsAvailable(floorPlan);
    contentVersionService.bump("PROJECTS");
    return ProjectFloorPlanInsightMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public FloorPlanInsightResponse update(Long projectId, Long floorPlanId, Long insightId, FloorPlanInsightUpsertRequest request) {
    ProjectFloorPlanEntity floorPlan = resolveFloorPlan(projectId, floorPlanId);

    ProjectFloorPlanInsightEntity entity = insightRepository.findByIdAndFloorPlanId(insightId, floorPlanId)
        .orElseThrow(() -> new NotFoundException("Insight not found: " + insightId));

    if (request.getInsightType() != null) entity.setInsightType(request.getInsightType());
    if (request.getTitle() != null) entity.setTitle(request.getTitle().trim());
    if (request.getSummary() != null) entity.setSummary(clean(request.getSummary()));
    if (request.getDetailedText() != null) entity.setDetailedText(clean(request.getDetailedText()));
    if (request.getUnitValue() != null) entity.setUnitValue(request.getUnitValue());
    if (request.getBenchmarkValue() != null) entity.setBenchmarkValue(request.getBenchmarkValue());
    if (request.getUnitLabel() != null) entity.setUnitLabel(clean(request.getUnitLabel()));
    if (request.getDifferenceValue() != null) entity.setDifferenceValue(request.getDifferenceValue());
    if (request.getDifferencePercent() != null) entity.setDifferencePercent(request.getDifferencePercent());
    if (request.getChartLabelThisUnit() != null) entity.setChartLabelThisUnit(clean(request.getChartLabelThisUnit()));
    if (request.getChartLabelAverage() != null) entity.setChartLabelAverage(clean(request.getChartLabelAverage()));
    if (request.getComparisonResult() != null) entity.setComparisonResult(request.getComparisonResult());
    if (request.getScore() != null) entity.setScore(request.getScore());
    if (request.getPositive() != null) entity.setPositive(request.getPositive());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getVerified() != null) entity.setVerified(request.getVerified());
    if (request.getActive() != null) entity.setActive(request.getActive());
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());

    ProjectFloorPlanInsightEntity saved = insightRepository.save(entity);

    recalculateInsightsAvailable(floorPlan);
    contentVersionService.bump("PROJECTS");
    return ProjectFloorPlanInsightMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long projectId, Long floorPlanId, Long insightId) {
    ProjectFloorPlanEntity floorPlan = resolveFloorPlan(projectId, floorPlanId);

    ProjectFloorPlanInsightEntity entity = insightRepository.findByIdAndFloorPlanId(insightId, floorPlanId)
        .orElseThrow(() -> new NotFoundException("Insight not found: " + insightId));

    // Soft delete
    entity.setDeleted(true);
    entity.setActive(false);
    insightRepository.save(entity);

    recalculateInsightsAvailable(floorPlan);
    contentVersionService.bump("PROJECTS");
  }

  @Override
  @Transactional(readOnly = true)
  public List<FloorPlanInsightResponse> list(Long projectId, Long floorPlanId) {
    resolveFloorPlan(projectId, floorPlanId);
    return insightRepository
        .findByFloorPlanIdAndDeletedFalseOrderBySortOrderAscIdAsc(floorPlanId)
        .stream()
        .map(ProjectFloorPlanInsightMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectFloorPlanInsightDetailResponse publicGetDetail(Long projectId, Long floorPlanId) {
    var project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

    ProjectFloorPlanEntity fp = floorPlanRepository.findByIdAndDeletedFalse(floorPlanId)
        .orElseThrow(() -> new NotFoundException("Floor plan not found: " + floorPlanId));

    if (!fp.getProject().getId().equals(projectId) || !Boolean.TRUE.equals(fp.getActive())) {
      throw new NotFoundException("Floor plan not found: " + floorPlanId);
    }

    List<FloorPlanRoomDimensionResponse> rooms = roomRepository
        .findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(floorPlanId)
        .stream()
        .map(ProjectFloorPlanRoomDimensionMapper::toResponse)
        .toList();

    List<FloorPlanInsightResponse> insights = insightRepository
        .findByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(floorPlanId)
        .stream()
        .map(ProjectFloorPlanInsightMapper::toResponse)
        .toList();

    return ProjectFloorPlanInsightDetailResponse.builder()
        .floorPlanId(fp.getId())
        .projectId(project.getId())
        .title(fp.getTitle())
        .imageUrl(fp.getImageUrl())
        .unitLabel(fp.getUnitLabel())
        .unitConfigurationType(fp.getUnitConfigurationType())
        .unitConfigurationTypeLabel(fp.getUnitConfigurationType() != null ? fp.getUnitConfigurationType().toLabel() : null)
        .price(fp.getPrice())
        .carpetAreaSqft(fp.getCarpetAreaSqft())
        .saleableAreaSqft(fp.getSaleableAreaSqft())
        .superAreaSqft(fp.getSuperAreaSqft())
        .carpetEfficiencyPercent(fp.getCarpetEfficiencyPercent())
        .bedrooms(fp.getBedrooms())
        .bathrooms(fp.getBathrooms())
        .balconies(fp.getBalconies())
        .facing(fp.getFacing())
        .directionSummary(fp.getDirectionSummary())
        .towerName(fp.getTowerName())
        .floorRange(fp.getFloorRange())
        .keyPlanImageUrl(fp.getKeyPlanImageUrl())
        .rooms(rooms)
        .insights(insights)
        .build();
  }

  private ProjectFloorPlanEntity resolveFloorPlan(Long projectId, Long floorPlanId) {
    ProjectFloorPlanEntity fp = floorPlanRepository.findByIdAndDeletedFalse(floorPlanId)
        .orElseThrow(() -> new NotFoundException("Floor plan not found: " + floorPlanId));
    if (!fp.getProject().getId().equals(projectId)) {
      throw new NotFoundException("Floor plan not found for project: " + floorPlanId);
    }
    return fp;
  }

  private void recalculateInsightsAvailable(ProjectFloorPlanEntity fp) {
    boolean hasPublicInsights = insightRepository
        .existsByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalse(fp.getId());
    if (!Boolean.valueOf(hasPublicInsights).equals(fp.getInsightsAvailable())) {
      fp.setInsightsAvailable(hasPublicInsights);
      floorPlanRepository.save(fp);
    }
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
