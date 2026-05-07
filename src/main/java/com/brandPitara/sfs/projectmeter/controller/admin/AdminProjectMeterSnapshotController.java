package com.brandPitara.sfs.projectmeter.controller.admin;

import com.brandPitara.sfs.projectmeter.service.ProjectMeterSnapshotRecalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/project-meter")
public class AdminProjectMeterSnapshotController {

    private final ProjectMeterSnapshotRecalculationService recalculationService;

    @PostMapping("/{projectId}/recalculate-snapshot")
    public ResponseEntity<String> recalculateSnapshot(@PathVariable Long projectId) {
        recalculationService.recalculateSnapshot(projectId);
        return ResponseEntity.ok("Project meter snapshot recalculated successfully for projectId=" + projectId);
    }

    @PostMapping("/recalculate-all")
    public ResponseEntity<String> recalculateAll() {
        recalculationService.recalculateAllPublishedSnapshots();
        return ResponseEntity.ok("All published project meter snapshots recalculated successfully.");
    }
}