package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.PublicCityDetailResponse;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.service.PublicCityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/cities")
@RequiredArgsConstructor
public class PublicCityController {

    private final PublicCityService publicCityService;

    @GetMapping("/trending")
    public List<TrendingCityCardResponse> trending(
            @RequestParam(required = false) Integer limit
    ) {
        return publicCityService.getTrendingCities(limit);
    }

    /**
     * GAP-017. A literal path segment ("/trending" above) always takes
     * precedence over a path-variable segment in Spring MVC's request
     * mapping resolution, so this can never shadow or be shadowed by the
     * route above — a request for /trending always resolves to
     * {@link #trending}, never here.
     */
    @GetMapping("/{citySlug}")
    public PublicCityDetailResponse getBySlug(@PathVariable String citySlug) {
        return publicCityService.getCityBySlug(citySlug);
    }
}
