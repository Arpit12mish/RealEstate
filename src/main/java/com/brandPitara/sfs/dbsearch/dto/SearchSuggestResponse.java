package com.brandPitara.sfs.dbsearch.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchSuggestResponse {
    private String query;
    private List<SearchSectionDto> sections;
}