package com.brandPitara.sfs.buildercredibility.controller.publicapi;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderCredibilityPublicController {

    private final BuilderCredibilityService builderCredibilityService;

    @GetMapping("/{builderId}/credibility")
    public BuilderCredibilityResponse credibility(@PathVariable Long builderId) {
        return builderCredibilityService.publicGetCredibility(builderId);
    }
}