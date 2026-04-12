package com.brandPitara.sfs.dbsearch.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchSectionDto {
    private String key;
    private String title;
    private List<SearchItemDto> items;
}