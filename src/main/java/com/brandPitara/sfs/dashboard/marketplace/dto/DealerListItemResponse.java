package com.brandPitara.sfs.dashboard.marketplace.dto;

public record DealerListItemResponse(
        Long id,
        String name,
        String categoryName,
        String cityName,
        String locality,
        boolean active
) {
}
