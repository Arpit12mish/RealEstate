package com.brandPitara.sfs.dashboard.instagram.controller;

import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelResponse;
import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelUpsertRequest;
import com.brandPitara.sfs.instagram.dto.InstagramReelSyncResult;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import com.brandPitara.sfs.instagram.service.InstagramReelService;
import com.brandPitara.sfs.instagram.service.InstagramReelSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/instagram-reels")
@RequiredArgsConstructor
public class DashboardInstagramReelController {

    private final InstagramReelService instagramReelService;
    private final InstagramReelSyncService instagramReelSyncService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public Page<DashboardInstagramReelResponse> list(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) InstagramReelCategory category,
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 50),
            Sort.by("displayOrder").ascending()
                .and(Sort.by("publishedAt").descending())
                .and(Sort.by("id").descending())
        );
        return instagramReelService.dashboardList(active, category, query, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardInstagramReelResponse get(@PathVariable Long id) {
        return instagramReelService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DashboardInstagramReelResponse create(
        @Valid @RequestBody DashboardInstagramReelUpsertRequest request
    ) {
        return instagramReelService.createManual(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DashboardInstagramReelResponse update(
        @PathVariable Long id,
        @Valid @RequestBody DashboardInstagramReelUpsertRequest request
    ) {
        return instagramReelService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardInstagramReelResponse setActive(
        @PathVariable Long id,
        @RequestParam boolean value
    ) {
        return instagramReelService.setActive(id, value);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        instagramReelService.softDelete(id);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public InstagramReelSyncResult sync() {
        return instagramReelSyncService.syncLatestReels();
    }

    @PostMapping("/recache")
    @PreAuthorize("hasRole('ADMIN')")
    public InstagramReelSyncResult recacheMissingThumbnails() {
        return instagramReelSyncService.recacheMissingThumbnails();
    }
}
