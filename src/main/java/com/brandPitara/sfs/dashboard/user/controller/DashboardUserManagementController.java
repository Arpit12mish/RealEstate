package com.brandPitara.sfs.dashboard.user.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserCmsPermissionsRequest;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserCreateRequest;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserPasswordRequest;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserResponse;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserStatusRequest;
import com.brandPitara.sfs.dashboard.user.service.DashboardUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardUserManagementController {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "email", "email",
            "displayName", "name",
            "role", "role",
            "active", "active",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private final DashboardUserManagementService userService;
    private final DashboardActionAuditService auditService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DashboardUserResponse create(@Valid @RequestBody DashboardUserCreateRequest request) {
        DashboardUserResponse response = userService.create(request);
        auditService.record(DashboardAuditAction.DASHBOARD_USER_CREATED,
                ReviewEntityType.DASHBOARD_USER, response.id(), null);
        return response;
    }

    @GetMapping
    public Page<DashboardUserResponse> list(
            @RequestParam(required = false) DashboardRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        String property = SORT_FIELDS.get(sortBy);
        if (property == null) {
            throw new IllegalArgumentException("Unsupported dashboard-user sort field: " + sortBy);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"))
        );
        return userService.list(role, active, search, pageable);
    }

    @GetMapping("/{userId}")
    public DashboardUserResponse get(@PathVariable Long userId) {
        return userService.get(userId);
    }

    @PutMapping("/{userId}/cms-permissions")
    public DashboardUserResponse updateCmsPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody DashboardUserCmsPermissionsRequest request
    ) {
        DashboardUserResponse response = userService.updateCmsPermissions(userId, request.permissionProfiles());
        auditService.record(DashboardAuditAction.DASHBOARD_USER_PERMISSIONS_CHANGED,
                ReviewEntityType.DASHBOARD_USER, userId, null);
        return response;
    }

    @PatchMapping("/{userId}/status")
    public DashboardUserResponse setStatus(
            @PathVariable Long userId,
            @Valid @RequestBody DashboardUserStatusRequest request
    ) {
        DashboardUserResponse response = userService.setActive(userId, request.active());
        auditService.record(
                request.active()
                        ? DashboardAuditAction.DASHBOARD_USER_ACTIVATED
                        : DashboardAuditAction.DASHBOARD_USER_DEACTIVATED,
                ReviewEntityType.DASHBOARD_USER,
                userId,
                null
        );
        return response;
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody DashboardUserPasswordRequest request
    ) {
        userService.resetPassword(userId, request.newPassword());
        auditService.record(DashboardAuditAction.DASHBOARD_USER_PASSWORD_RESET,
                ReviewEntityType.DASHBOARD_USER, userId, null);
    }
}
