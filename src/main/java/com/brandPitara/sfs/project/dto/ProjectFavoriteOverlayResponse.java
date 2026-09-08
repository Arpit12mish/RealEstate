package com.brandPitara.sfs.project.dto;

import java.util.List;

public record ProjectFavoriteOverlayResponse(List<ProjectFavoriteOverlayItemResponse> items) {
    public ProjectFavoriteOverlayResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
