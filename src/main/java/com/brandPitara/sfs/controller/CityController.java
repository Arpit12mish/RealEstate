package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    /**
     * Example:
     *   GET /api/cities        -> top 50 cities ordered by name
     *   GET /api/cities?q=del  -> cities containing "del" (Delhi, etc.)
     */
    @GetMapping
    public List<CityResponse> search(@RequestParam(value = "q", required = false) String query) {
        return cityService.searchCities(query);
    }
}
