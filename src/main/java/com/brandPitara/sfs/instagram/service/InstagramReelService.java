package com.brandPitara.sfs.instagram.service;

import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelResponse;
import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelUpsertRequest;
import com.brandPitara.sfs.instagram.dto.PublicInstagramReelItemResponse;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InstagramReelService {

    Page<DashboardInstagramReelResponse> dashboardList(
        Boolean active,
        InstagramReelCategory category,
        String query,
        Pageable pageable
    );

    DashboardInstagramReelResponse get(Long id);

    DashboardInstagramReelResponse createManual(DashboardInstagramReelUpsertRequest request);

    DashboardInstagramReelResponse update(Long id, DashboardInstagramReelUpsertRequest request);

    DashboardInstagramReelResponse setActive(Long id, boolean active);

    void softDelete(Long id);

    List<PublicInstagramReelItemResponse> publicList(InstagramReelCategory category, int page, int limit);

    List<PublicInstagramReelItemResponse> publicHomeItems(Integer requestedLimit);
}
