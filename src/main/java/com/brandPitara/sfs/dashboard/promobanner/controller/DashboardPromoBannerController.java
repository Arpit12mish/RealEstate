package com.brandPitara.sfs.dashboard.promobanner.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerResponse;
import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerUpsertRequest;
import com.brandPitara.sfs.dashboard.promobanner.service.DashboardPromoBannerService;
import com.brandPitara.sfs.enums.PromoBannerMediaType;
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
@RequestMapping("/api/dashboard/promo-banners")
@RequiredArgsConstructor
public class DashboardPromoBannerController {

    private final DashboardPromoBannerService promoBannerService;
    private final DashboardActionAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public Page<DashboardPromoBannerResponse> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String slotKey,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) PromoBannerMediaType mediaType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by("priority").ascending().and(Sort.by("id").ascending())
        );
        return promoBannerService.list(categoryId, slotKey, active, mediaType, pageable);
    }

    @GetMapping("/{bannerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardPromoBannerResponse get(@PathVariable Long bannerId) {
        return promoBannerService.get(bannerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DashboardPromoBannerResponse create(
            @Valid @RequestBody DashboardPromoBannerUpsertRequest request
    ) {
        DashboardPromoBannerResponse response = promoBannerService.create(request);
        auditService.record(
                DashboardAuditAction.PROMO_BANNER_CREATED,
                ReviewEntityType.PROMO_BANNER,
                response.getId(),
                null
        );
        return response;
    }

    @PutMapping("/{bannerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DashboardPromoBannerResponse update(
            @PathVariable Long bannerId,
            @Valid @RequestBody DashboardPromoBannerUpsertRequest request
    ) {
        DashboardPromoBannerResponse response = promoBannerService.update(bannerId, request);
        auditService.record(
                DashboardAuditAction.PROMO_BANNER_UPDATED,
                ReviewEntityType.PROMO_BANNER,
                bannerId,
                null
        );
        return response;
    }

    @PatchMapping("/{bannerId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardPromoBannerResponse setActive(
            @PathVariable Long bannerId,
            @RequestParam boolean value
    ) {
        DashboardPromoBannerResponse response = promoBannerService.setActive(bannerId, value);
        auditService.record(
                value ? DashboardAuditAction.PROMO_BANNER_ACTIVATED : DashboardAuditAction.PROMO_BANNER_DEACTIVATED,
                ReviewEntityType.PROMO_BANNER,
                bannerId,
                null
        );
        return response;
    }

    @DeleteMapping("/{bannerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long bannerId) {
        promoBannerService.softDelete(bannerId);
        auditService.record(
                DashboardAuditAction.PROMO_BANNER_DELETED,
                ReviewEntityType.PROMO_BANNER,
                bannerId,
                null
        );
    }
}
