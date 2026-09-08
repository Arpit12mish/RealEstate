package com.brandPitara.sfs.dashboard.promobanner.service;

import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerResponse;
import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerUpsertRequest;
import com.brandPitara.sfs.enums.PromoBannerMediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DashboardPromoBannerService {
    Page<DashboardPromoBannerResponse> list(
            Long categoryId,
            String slotKey,
            Boolean active,
            PromoBannerMediaType mediaType,
            Pageable pageable
    );

    DashboardPromoBannerResponse get(Long bannerId);

    DashboardPromoBannerResponse create(DashboardPromoBannerUpsertRequest request);

    DashboardPromoBannerResponse update(Long bannerId, DashboardPromoBannerUpsertRequest request);

    DashboardPromoBannerResponse setActive(Long bannerId, boolean active);

    void softDelete(Long bannerId);
}
