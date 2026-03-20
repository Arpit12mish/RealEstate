package com.brandPitara.sfs.projectmeter.controller.publicapi;

import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/project-meter")
@RequiredArgsConstructor
public class ProjectMeterCardPublicController {

    private final ProjectMeterService projectMeterService;

    @GetMapping("/cards")
    public Page<ProjectMeterCardResponse> cards(
        @RequestParam(required = false) Long cityId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(
            page,
            Math.min(size, 20),
            Sort.by("priority").ascending().and(Sort.by("id").descending())
        );

        return projectMeterService.publicListMeterCards(cityId, pageable);
    }
}