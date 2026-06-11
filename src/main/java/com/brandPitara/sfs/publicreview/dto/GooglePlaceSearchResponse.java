package com.brandPitara.sfs.publicreview.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GooglePlaceSearchResponse {
    private Long projectId;
    private String query;
    private List<GooglePlaceSearchResultItem> results;
}
