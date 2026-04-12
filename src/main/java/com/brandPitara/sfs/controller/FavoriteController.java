package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/project-favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final ProjectFavoriteService projectFavoriteService;

    @PostMapping("/{projectId}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long projectId) {
        projectFavoriteService.toggleProjectFavorite(projectId);

        boolean isFavorite = projectFavoriteService.isProjectFavorite(projectId);
        long favoriteCount = projectFavoriteService.getProjectFavoriteCount(projectId);

        return ResponseEntity.ok(Map.of(
                "targetType", "PROJECT",
                "targetId", projectId,
                "isFavorite", isFavorite,
                "favoriteCount", favoriteCount
        ));
    }

    @GetMapping("/{projectId}/exists")
    public ResponseEntity<?> exists(@PathVariable Long projectId) {
        return ResponseEntity.ok(Map.of(
                "isFavorite", projectFavoriteService.isProjectFavorite(projectId)
        ));
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(projectFavoriteService.listMyFavoriteProjects(pageable));
    }
}