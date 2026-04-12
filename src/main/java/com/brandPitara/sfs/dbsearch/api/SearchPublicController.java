package com.brandPitara.sfs.dbsearch.api;

import com.brandPitara.sfs.dbsearch.app.SearchService;
import com.brandPitara.sfs.dbsearch.dto.SearchResultResponse;
import com.brandPitara.sfs.dbsearch.dto.SearchSuggestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/search")
@RequiredArgsConstructor
public class SearchPublicController {

    private final SearchService searchService;

    @GetMapping("/suggest")
    public SearchSuggestResponse suggest(
            @RequestParam String q,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Integer limit
    ) {
        return searchService.suggest(q, cityId, limit);
    }

    @GetMapping
    public SearchResultResponse search(
            @RequestParam String q,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return searchService.search(q, cityId, page, size);
    }
}