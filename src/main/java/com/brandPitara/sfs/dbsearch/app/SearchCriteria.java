package com.brandPitara.sfs.dbsearch.app;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;

import java.time.LocalDate;
import java.util.List;

public record SearchCriteria(
        String query,
        Long cityId,
        String citySlug,
        Long priceMin,
        Long priceMax,
        Integer constructionProgressMin,
        Integer constructionProgressMax,
        LocalDate possessionFrom,
        LocalDate possessionTo,
        List<ProjectStatus> statuses,
        List<PropertyType> propertyTypes,
        SearchSort sort,
        Integer page,
        Integer size
) {
    public SearchCriteria(String query, Long cityId, String citySlug, Integer page, Integer size) {
        this(
                query,
                cityId,
                citySlug,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                page,
                size
        );
    }
}
