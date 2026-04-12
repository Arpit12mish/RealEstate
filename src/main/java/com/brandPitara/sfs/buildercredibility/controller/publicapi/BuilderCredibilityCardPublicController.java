package com.brandPitara.sfs.buildercredibility.controller.publicapi;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityCardResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderCredibilityCardPublicController {

    private final BuilderCredibilityService builderCredibilityService;

    @GetMapping("/credibility/cards")
    public List<BuilderCredibilityCardResponse> cards(
        @RequestParam(required = false) Long cityId,
        @RequestParam(defaultValue = "12") int limit
    ) {
        return builderCredibilityService.publicListCredibilityCards(cityId, limit);
    }
}