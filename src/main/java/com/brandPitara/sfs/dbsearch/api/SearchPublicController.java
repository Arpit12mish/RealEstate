package com.brandPitara.sfs.dbsearch.api;

import com.brandPitara.sfs.dbsearch.app.SearchService;
import com.brandPitara.sfs.dbsearch.app.SearchCriteria;
import com.brandPitara.sfs.dbsearch.app.SearchSort;
import com.brandPitara.sfs.dbsearch.dto.SearchResultResponse;
import com.brandPitara.sfs.dbsearch.dto.SearchSuggestResponse;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/public/search")
@RequiredArgsConstructor
public class SearchPublicController {

    private final SearchService searchService;

    @GetMapping("/suggest")
    public SearchSuggestResponse suggest(
            @RequestParam String q,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String citySlug,
            @RequestParam(required = false) Integer limit
    ) {
        return searchService.suggest(q, cityId, citySlug, limit);
    }

    @GetMapping
    public SearchResultResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String citySlug,
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) Integer constructionProgressMin,
            @RequestParam(required = false) Integer constructionProgressMax,
            @RequestParam(required = false) String possessionFrom,
            @RequestParam(required = false) String possessionTo,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> propertyTypes,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return searchService.search(new SearchCriteria(
                q,
                cityId,
                citySlug,
                priceMin,
                priceMax,
                constructionProgressMin,
                constructionProgressMax,
                parseDate(possessionFrom, "possessionFrom"),
                parseDate(possessionTo, "possessionTo"),
                parseEnums(statuses, ProjectStatus.class, "statuses"),
                parseEnums(propertyTypes, PropertyType.class, "propertyTypes"),
                parseEnum(sort, SearchSort.class, "sort"),
                page,
                size
        ));
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " must be a valid ISO date in YYYY-MM-DD format"
            );
        }
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid " + fieldName + " value: " + value.trim()
            );
        }
    }

    private <E extends Enum<E>> List<E> parseEnums(
            List<String> values,
            Class<E> enumType,
            String fieldName
    ) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<E> parsed = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            for (String part : value.split(",")) {
                E enumValue = parseEnum(part, enumType, fieldName);
                if (enumValue != null && !parsed.contains(enumValue)) {
                    parsed.add(enumValue);
                }
            }
        }
        return parsed;
    }
}
