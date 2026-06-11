package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;
import com.brandPitara.sfs.project.mapper.ProjectFloorPlanRoomDimensionMapper;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRoomDimensionRepository;
import com.brandPitara.sfs.project.service.ProjectFloorPlanRoomDimensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFloorPlanRoomDimensionServiceImpl implements ProjectFloorPlanRoomDimensionService {

  private final ProjectFloorPlanRepository floorPlanRepository;
  private final ProjectFloorPlanRoomDimensionRepository roomRepository;
  private final ContentVersionService contentVersionService;

  @Override
  @Transactional
  public FloorPlanRoomDimensionResponse create(Long projectId, Long floorPlanId, FloorPlanRoomDimensionUpsertRequest request) {
    ProjectFloorPlanEntity floorPlan = resolveFloorPlan(projectId, floorPlanId);

    ProjectFloorPlanRoomDimensionEntity entity = ProjectFloorPlanRoomDimensionEntity.builder()
        .floorPlan(floorPlan)
        .roomType(request.getRoomType())
        .label(clean(request.getLabel()))
        .lengthFt(request.getLengthFt())
        .widthFt(request.getWidthFt())
        .areaSqft(request.getAreaSqft())
        .dimensionText(clean(request.getDimensionText()))
        .iconKey(clean(request.getIconKey()))
        .notes(clean(request.getNotes()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    ProjectFloorPlanRoomDimensionEntity saved = roomRepository.save(entity);
    contentVersionService.bump("PROJECTS");
    return ProjectFloorPlanRoomDimensionMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public FloorPlanRoomDimensionResponse update(Long projectId, Long floorPlanId, Long roomId, FloorPlanRoomDimensionUpsertRequest request) {
    resolveFloorPlan(projectId, floorPlanId);

    ProjectFloorPlanRoomDimensionEntity entity = roomRepository
        .findByIdAndFloorPlanIdAndDeletedFalse(roomId, floorPlanId)
        .orElseThrow(() -> new NotFoundException("Room dimension not found: " + roomId));

    if (request.getRoomType() != null) entity.setRoomType(request.getRoomType());
    if (request.getLabel() != null) entity.setLabel(clean(request.getLabel()));
    if (request.getLengthFt() != null) entity.setLengthFt(request.getLengthFt());
    if (request.getWidthFt() != null) entity.setWidthFt(request.getWidthFt());
    if (request.getAreaSqft() != null) entity.setAreaSqft(request.getAreaSqft());
    if (request.getDimensionText() != null) entity.setDimensionText(clean(request.getDimensionText()));
    if (request.getIconKey() != null) entity.setIconKey(clean(request.getIconKey()));
    if (request.getNotes() != null) entity.setNotes(clean(request.getNotes()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    ProjectFloorPlanRoomDimensionEntity saved = roomRepository.save(entity);
    contentVersionService.bump("PROJECTS");
    return ProjectFloorPlanRoomDimensionMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long projectId, Long floorPlanId, Long roomId) {
    resolveFloorPlan(projectId, floorPlanId);

    ProjectFloorPlanRoomDimensionEntity entity = roomRepository
        .findByIdAndFloorPlanIdAndDeletedFalse(roomId, floorPlanId)
        .orElseThrow(() -> new NotFoundException("Room dimension not found: " + roomId));

    // Soft delete
    entity.setDeleted(true);
    entity.setActive(false);
    roomRepository.save(entity);
    contentVersionService.bump("PROJECTS");
  }

  @Override
  @Transactional(readOnly = true)
  public List<FloorPlanRoomDimensionResponse> list(Long projectId, Long floorPlanId) {
    resolveFloorPlan(projectId, floorPlanId);
    return roomRepository.findByFloorPlanIdAndDeletedFalseOrderBySortOrderAscIdAsc(floorPlanId)
        .stream()
        .map(ProjectFloorPlanRoomDimensionMapper::toResponse)
        .toList();
  }

  private ProjectFloorPlanEntity resolveFloorPlan(Long projectId, Long floorPlanId) {
    ProjectFloorPlanEntity fp = floorPlanRepository.findByIdAndDeletedFalse(floorPlanId)
        .orElseThrow(() -> new NotFoundException("Floor plan not found: " + floorPlanId));
    if (!fp.getProject().getId().equals(projectId)) {
      throw new NotFoundException("Floor plan not found for project: " + floorPlanId);
    }
    return fp;
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
