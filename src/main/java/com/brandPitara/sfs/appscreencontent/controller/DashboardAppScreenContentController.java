package com.brandPitara.sfs.appscreencontent.controller;

import com.brandPitara.sfs.appscreencontent.dto.AppScreenContentRequest;
import com.brandPitara.sfs.appscreencontent.dto.AppScreenContentResponse;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;
import com.brandPitara.sfs.appscreencontent.service.AppScreenContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/app-screen-content")
@RequiredArgsConstructor
public class DashboardAppScreenContentController {

    private final AppScreenContentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public AppScreenContentResponse create(@Valid @RequestBody AppScreenContentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public AppScreenContentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AppScreenContentRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public AppScreenContentResponse setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) {
        return service.setEnabled(id, enabled);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<AppScreenContentResponse> list(
            @RequestParam AppScreenKey screenKey,
            @RequestParam(required = false) AppScreenPlacement placement
    ) {
        return service.list(screenKey, placement);
    }
}
