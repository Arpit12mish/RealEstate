package com.brandPitara.sfs.controller;

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
}
