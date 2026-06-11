package com.brandPitara.sfs.project.controller.admin;

import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectFloorPlanRoomDimensionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/floor-plans/{floorPlanId}/rooms")
@RequiredArgsConstructor
public class AdminProjectFloorPlanRoomDimensionController {

  private final ProjectFloorPlanRoomDimensionService roomDimensionService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<FloorPlanRoomDimensionResponse> list(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId
  ) {
    return roomDimensionService.list(projectId, floorPlanId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public FloorPlanRoomDimensionResponse create(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId,
      @Valid @RequestBody FloorPlanRoomDimensionUpsertRequest request
  ) {
    return roomDimensionService.create(projectId, floorPlanId, request);
  }

  @PutMapping("/{roomId}")
  @PreAuthorize("hasRole('ADMIN')")
  public FloorPlanRoomDimensionResponse update(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId,
      @PathVariable Long roomId,
      @Valid @RequestBody FloorPlanRoomDimensionUpsertRequest request
  ) {
    return roomDimensionService.update(projectId, floorPlanId, roomId, request);
  }

  @DeleteMapping("/{roomId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId,
      @PathVariable Long roomId
  ) {
    roomDimensionService.delete(projectId, floorPlanId, roomId);
  }
}
