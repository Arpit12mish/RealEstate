package com.brandPitara.sfs.builderhighlight.controller.publicapi;

import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightPublicItemResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightsScreenResponse;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.builderhighlight.service.BuilderHighlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builders/{builderId}/highlights")
@RequiredArgsConstructor
public class BuilderHighlightPublicController {

    private final BuilderHighlightService builderHighlightService;

    @GetMapping
    public BuilderHighlightsScreenResponse screen(@PathVariable Long builderId) {
        return builderHighlightService.publicGetScreen(builderId);
    }

    @GetMapping("/items")
    public Page<BuilderHighlightPublicItemResponse> items(
        @PathVariable Long builderId,
        @RequestParam BuilderHighlightType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return builderHighlightService.publicListItems(
            builderId,
            type,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 20))
        );
    }

    @GetMapping("/items/{itemId}")
    public BuilderHighlightPublicItemResponse item(
        @PathVariable Long builderId,
        @PathVariable Long itemId
    ) {
        return builderHighlightService.publicGetItem(builderId, itemId);
    }
}
