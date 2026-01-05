package com.brandPitara.sfs.provider.dto;

import java.util.List;

public record ProviderDashboardResponse(
        Long providerId,
        String providerType,
        String displayName,
        String primaryCategoryName,
        int completionPercent,
        List<String> missing,
        String recommendedNext,
        Counts counts,

        // ✅ NEW
        long openRequestsCount,
        List<ServiceRequestCard> latestRequests
) {
    public record Counts(
            long galleryImages,
            long projects,
            long serviceAreas,
            boolean hasProfilePhoto
    ) {}

    // ✅ Minimal card for Provider dashboard UI
    public record ServiceRequestCard(
            Long id,
            String customerName,
            String requirement, // category names combined
            String locality,
            String pincode
    ) {}
}
