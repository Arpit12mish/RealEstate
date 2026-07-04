package com.brandPitara.sfs.projectcompare.controller;

import com.brandPitara.sfs.projectcompare.dto.request.ProjectComparisonRequest;
import com.brandPitara.sfs.projectcompare.dto.response.ProjectComparisonResponse;
import com.brandPitara.sfs.projectcompare.service.ProjectComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectComparisonPublicController {

    private final ProjectComparisonService projectComparisonService;

    @PostMapping("/compare")
    public ResponseEntity<ProjectComparisonResponse> compare(
            @Valid @RequestBody ProjectComparisonRequest request
    ) {
        return ResponseEntity.ok(projectComparisonService.compare(request));
    }
}
