package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectPublicV2Response;
import com.brandPitara.sfs.project.mapper.ProjectPublicV2Mapper;
import com.brandPitara.sfs.project.service.ProjectPublicCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/public/projects")
@RequiredArgsConstructor
public class ProjectPublicV2Controller {

    static final String PROJECT_DETAIL_CACHE_CONTROL =
            "public, max-age=0, s-maxage=30, stale-while-revalidate=30, stale-if-error=60";

    private final ProjectPublicCoreService projectPublicCoreService;

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectPublicV2Response> get(@PathVariable Long projectId) {
        var core = projectPublicCoreService.getById(projectId);
        if (core.getFailedSections() != null && !core.getFailedSections().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Project detail temporarily unavailable"
            );
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, PROJECT_DETAIL_CACHE_CONTROL)
                .body(ProjectPublicV2Mapper.toResponse(core));
    }
}
