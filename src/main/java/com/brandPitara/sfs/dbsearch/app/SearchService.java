package com.brandPitara.sfs.dbsearch.app;

import com.brandPitara.sfs.dbsearch.dto.SearchResultResponse;
import com.brandPitara.sfs.dbsearch.dto.SearchSuggestResponse;

public interface SearchService {
    SearchSuggestResponse suggest(String query, Long cityId, String citySlug, Integer limit);
    SearchResultResponse search(String query, Long cityId, Integer page, Integer size);
    SearchResultResponse search(SearchCriteria criteria);
}
