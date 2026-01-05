package com.brandPitara.sfs.provider.dto;

import java.util.List;

public record ProviderMediaListResponse(
        ProviderMediaResponse profilePhoto,
        List<ProviderMediaResponse> gallery
) {}
