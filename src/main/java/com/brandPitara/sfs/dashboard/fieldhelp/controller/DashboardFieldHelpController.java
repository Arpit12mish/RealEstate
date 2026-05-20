package com.brandPitara.sfs.dashboard.fieldhelp.controller;

import com.brandPitara.sfs.dashboard.common.enums.DashboardHelpModule;
import com.brandPitara.sfs.dashboard.fieldhelp.dto.DashboardFieldHelpResponse;
import com.brandPitara.sfs.dashboard.fieldhelp.dto.DashboardFieldHelpUpsertRequest;
import com.brandPitara.sfs.dashboard.fieldhelp.service.DashboardFieldHelpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/field-help")
@RequiredArgsConstructor
public class DashboardFieldHelpController {

    private final DashboardFieldHelpService fieldHelpService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<DashboardFieldHelpResponse> listByModule(@RequestParam DashboardHelpModule module) {
        return fieldHelpService.listByModule(module);
    }

    @GetMapping("/{module}/{fieldKey}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardFieldHelpResponse getOne(
            @PathVariable DashboardHelpModule module,
            @PathVariable String fieldKey
    ) {
        return fieldHelpService.getOne(module, fieldKey);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardFieldHelpResponse upsert(@Valid @RequestBody DashboardFieldHelpUpsertRequest request) {
        return fieldHelpService.upsert(request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public void setActive(@PathVariable Long id, @RequestParam boolean value) {
        fieldHelpService.setActive(id, value);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        fieldHelpService.delete(id);
    }
}
