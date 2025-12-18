package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.service.PromoBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories/{categoryId}/banners")
@RequiredArgsConstructor
public class PromoBannerController {

    private final PromoBannerService promoBannerService;

    @GetMapping
    public List<PromoBannerResponse> list(@PathVariable Long categoryId) {
        return promoBannerService.getBannersForCategory(categoryId);
    }
}
