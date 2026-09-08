package com.brandPitara.sfs.dashboard.user.service.impl;

import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardRefreshTokenService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserCreateRequest;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserResponse;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.dashboard.user.service.DashboardUserManagementService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.security.identity.AuthenticationIdentityCacheInvalidator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DashboardUserManagementServiceImpl implements DashboardUserManagementService {

    private final DashboardUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DashboardRefreshTokenService refreshTokenService;
    private final AuthenticationIdentityCacheInvalidator identityCacheInvalidator;
    private final DashboardCurrentUserService currentUserService;

    @Override
    @Transactional
    public DashboardUserResponse create(DashboardUserCreateRequest request) {
        if (request.role() != DashboardRole.CONTENT_STAFF) {
            throw new IllegalArgumentException("Only CONTENT_STAFF accounts may be created by this endpoint.");
        }
        validateStrongPassword(request.initialPassword());

        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dashboard user email already exists.");
        }

        DashboardUserEntity user = DashboardUserEntity.builder()
                .email(normalizedEmail)
                .name(request.displayName().trim())
                .passwordHash(passwordEncoder.encode(request.initialPassword()))
                .role(DashboardRole.CONTENT_STAFF)
                .permissions(new LinkedHashSet<>(CmsPermissionProfile.resolve(request.permissionProfiles())))
                .active(true)
                .build();

        DashboardUserEntity saved = userRepository.saveAndFlush(user);
        identityCacheInvalidator.invalidateDashboardAfterCommit(saved.getId());
        return DashboardUserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardUserResponse> list(
            DashboardRole role,
            Boolean active,
            String search,
            Pageable pageable
    ) {
        String normalizedSearch = StringUtils.hasText(search)
                ? search.trim().toLowerCase(Locale.ROOT)
                : null;
        Specification<DashboardUserEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (normalizedSearch != null) {
                String pattern = "%" + escapeLike(normalizedSearch) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(cb.lower(root.get("name")), pattern, '\\')
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return userRepository.findAll(specification, pageable).map(DashboardUserResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardUserResponse get(Long userId) {
        return DashboardUserResponse.from(findUser(userId));
    }

    @Override
    @Transactional
    public DashboardUserResponse updateCmsPermissions(Long userId, Set<CmsPermissionProfile> profiles) {
        DashboardUserEntity user = findUserForUpdate(userId);
        requireContentStaff(user);

        Set<DashboardPermission> resolved = CmsPermissionProfile.resolve(profiles);
        user.getPermissions().clear();
        user.getPermissions().addAll(resolved);
        user.setUpdatedAt(OffsetDateTime.now());
        DashboardUserEntity saved = userRepository.saveAndFlush(user);
        identityCacheInvalidator.invalidateDashboardAfterCommit(userId);
        return DashboardUserResponse.from(saved);
    }

    @Override
    @Transactional
    public DashboardUserResponse setActive(Long userId, boolean active) {
        DashboardUserEntity actor = currentUserService.getCurrentUserOrThrow();
        DashboardUserEntity candidate = findUser(userId);

        if (!active && actor.getId().equals(userId)) {
            throw new IllegalStateException("An administrator cannot deactivate their own account.");
        }
        DashboardUserEntity user;
        if (!active && candidate.getRole() == DashboardRole.ADMIN && candidate.isActiveUser()) {
            // Lock every active administrator in stable ID order. This makes the
            // final-admin check atomic even when two admins act concurrently.
            List<DashboardUserEntity> activeAdministrators = userRepository.lockActiveAdministrators();
            if (activeAdministrators.size() <= 1) {
                throw new IllegalStateException("The final active ADMIN account cannot be deactivated.");
            }
            user = activeAdministrators.stream()
                    .filter(administrator -> administrator.getId().equals(userId))
                    .findFirst()
                    .orElseGet(() -> findUserForUpdate(userId));
        } else {
            user = findUserForUpdate(userId);
        }

        user.setActive(active);
        user.setUpdatedAt(OffsetDateTime.now());
        DashboardUserEntity saved = userRepository.saveAndFlush(user);
        if (!active) {
            refreshTokenService.revokeAllForUser(userId);
        }
        identityCacheInvalidator.invalidateDashboardAfterCommit(userId);
        return DashboardUserResponse.from(saved);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        validateStrongPassword(newPassword);
        DashboardUserEntity user = findUserForUpdate(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
        identityCacheInvalidator.invalidateDashboardAfterCommit(userId);
    }

    private DashboardUserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Dashboard user not found: " + userId));
    }

    private DashboardUserEntity findUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("Dashboard user not found: " + userId));
    }

    private void requireContentStaff(DashboardUserEntity user) {
        if (user.getRole() != DashboardRole.CONTENT_STAFF) {
            throw new IllegalArgumentException("CMS permission profiles may only be assigned to CONTENT_STAFF accounts.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void validateStrongPassword(String password) {
        boolean valid = password != null
                && password.length() >= 12
                && password.length() <= 128
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!valid) {
            throw new IllegalArgumentException(
                    "Password must be 12-128 characters and include upper-case, lower-case, numeric, and special characters."
            );
        }
    }
}
