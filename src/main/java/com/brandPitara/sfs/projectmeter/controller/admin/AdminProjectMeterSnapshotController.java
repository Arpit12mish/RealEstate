package com.brandPitara.sfs.projectmeter.controller.admin;

import com.brandPitara.sfs.projectmeter.service.ProjectMeterSnapshotRecalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/project-meter")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjectMeterSnapshotController {

    private final ProjectMeterSnapshotRecalculationService recalculationService;

    @PostMapping("/{projectId}/recalculate-snapshot")
    public ResponseEntity<String> recalculateSnapshot(@PathVariable Long projectId) {
        recalculationService.recalculateSnapshot(projectId);
        return ResponseEntity.ok("Project meter snapshot recalculated successfully for projectId=" + projectId);
    }

    @PostMapping("/recalculate-all")
    public ResponseEntity<String> recalculateAll(
            @RequestHeader(value = "X-Confirm", required = false) String confirm
    ) {
        if (!"RECALCULATE_ALL".equals(confirm)) {
            return ResponseEntity.badRequest()
                    .body("This operation requires the request header: X-Confirm: RECALCULATE_ALL");
        }
        recalculationService.recalculateAllPublishedSnapshots();
        return ResponseEntity.ok("All published project meter snapshots recalculated successfully.");
    }
}