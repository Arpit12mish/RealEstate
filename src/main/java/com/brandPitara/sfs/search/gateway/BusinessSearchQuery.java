package com.brandPitara.sfs.search.gateway;

/**
 * App-owned search request for {@link BusinessSearchGateway#search}. {@code size}
 * is expected to already be clamped by the caller (BusinessSearchServiceImpl).
 */
public record BusinessSearchQuery(
        Long cityId,
        Long categoryId,
        String text,
        Double userLat,
        Double userLon,
        int page,
        int size
) {}
