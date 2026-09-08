package com.brandPitara.sfs.project.controller;

import com.brandPitara.sfs.project.dto.ProjectFavoriteOverlayResponse;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/project-favorites")
@RequiredArgsConstructor
@PreAuthorize("!hasRole('GUEST')")
public class ProjectFavoriteOverlayController {

    private final ProjectFavoriteService projectFavoriteService;

    @GetMapping
    public ProjectFavoriteOverlayResponse get(
            @RequestParam List<Long> projectIds
    ) {
        return projectFavoriteService.getCurrentViewerFavoriteOverlay(projectIds);
    }
}
