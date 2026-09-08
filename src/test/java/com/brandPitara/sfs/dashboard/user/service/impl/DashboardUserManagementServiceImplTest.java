package com.brandPitara.sfs.dashboard.user.service.impl;

import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardRefreshTokenService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserCreateRequest;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.security.identity.AuthenticationIdentityCacheInvalidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.brandPitara.sfs.dashboard.common.enums.DashboardPermission.CMS_CONTENT_EDIT_ANY;
import static com.brandPitara.sfs.dashboard.common.enums.DashboardPermission.CMS_CONTENT_PUBLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardUserManagementServiceImplTest {

    @Mock private DashboardUserRepository userRepository;
    @Mock private DashboardRefreshTokenService refreshTokenService;
    @Mock private AuthenticationIdentityCacheInvalidator identityCacheInvalidator;
    @Mock private DashboardCurrentUserService currentUserService;

    private PasswordEncoder passwordEncoder;
    private DashboardUserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new DashboardUserManagementServiceImpl(
                userRepository,
                passwordEncoder,
                refreshTokenService,
                identityCacheInvalidator,
                currentUserService
        );
    }

    @Test
    void createsActiveContentStaffWithNormalizedEmailHashedPasswordAndResolvedProfiles() {
        when(userRepository.existsByEmailIgnoreCase("writer@squarefootstory.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            DashboardUserEntity user = invocation.getArgument(0);
            user.setId(41L);
            return user;
        });

        var response = service.create(new DashboardUserCreateRequest(
                "  Writer@SquareFootStory.com ",
                " Content Writer ",
                DashboardRole.CONTENT_STAFF,
                Set.of(CmsPermissionProfile.WRITER),
                "Strong!Pass123"
        ));

        ArgumentCaptor<DashboardUserEntity> captor = ArgumentCaptor.forClass(DashboardUserEntity.class);
        verify(userRepository).saveAndFlush(captor.capture());
        DashboardUserEntity saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("writer@squarefootstory.com");
        assertThat(saved.getName()).isEqualTo("Content Writer");
        assertThat(saved.getRole()).isEqualTo(DashboardRole.CONTENT_STAFF);
        assertThat(saved.isActiveUser()).isTrue();
        assertThat(passwordEncoder.matches("Strong!Pass123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getPasswordHash()).doesNotContain("Strong!Pass123");
        assertThat(saved.getPermissions()).containsExactlyInAnyOrderElementsOf(CmsPermissionProfile.WRITER.permissions());
        assertThat(response.permissionProfiles()).containsExactly(CmsPermissionProfile.WRITER);
        verify(identityCacheInvalidator).invalidateDashboardAfterCommit(41L);
    }

    @Test
    void rejectsDuplicateEmailBeforeHashingOrSaving() {
        when(userRepository.existsByEmailIgnoreCase("writer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new DashboardUserCreateRequest(
                "Writer@example.com", "Writer", DashboardRole.CONTENT_STAFF,
                Set.of(CmsPermissionProfile.WRITER), "Strong!Pass123"
        ))).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNonContentStaffRoleAndWeakPassword() {
        assertThatThrownBy(() -> service.create(new DashboardUserCreateRequest(
                "admin@example.com", "Admin", DashboardRole.ADMIN,
                Set.of(), "Strong!Pass123"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only CONTENT_STAFF");

        assertThatThrownBy(() -> service.create(new DashboardUserCreateRequest(
                "writer@example.com", "Writer", DashboardRole.CONTENT_STAFF,
                Set.of(), "too-short"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12-128");
    }

    @Test
    void combinesProfilesWithoutDuplicatingPermissionsAndInvalidatesIdentity() {
        DashboardUserEntity user = contentStaff(52L, true);
        when(userRepository.findByIdForUpdate(52L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        var response = service.updateCmsPermissions(
                52L,
                Set.of(CmsPermissionProfile.EDITOR, CmsPermissionProfile.PUBLISHER)
        );

        assertThat(response.permissions()).contains(CMS_CONTENT_EDIT_ANY, CMS_CONTENT_PUBLISH);
        assertThat(response.permissions()).hasSize(
                CmsPermissionProfile.resolve(Set.of(CmsPermissionProfile.EDITOR, CmsPermissionProfile.PUBLISHER)).size()
        );
        assertThat(response.permissionProfiles())
                .containsExactlyInAnyOrder(CmsPermissionProfile.EDITOR, CmsPermissionProfile.PUBLISHER);
        verify(identityCacheInvalidator).invalidateDashboardAfterCommit(52L);
    }

    @Test
    void rejectsCmsPermissionAssignmentToLegacyDashboardRole() {
        DashboardUserEntity reviewer = user(53L, DashboardRole.REVIEWER, true);
        when(userRepository.findByIdForUpdate(53L)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> service.updateCmsPermissions(53L, Set.of(CmsPermissionProfile.WRITER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONTENT_STAFF");
    }

    @Test
    void deactivationRevokesAllRefreshTokensAndInvalidatesIdentity() {
        DashboardUserEntity actor = user(1L, DashboardRole.ADMIN, true);
        DashboardUserEntity target = contentStaff(61L, true);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(actor);
        when(userRepository.findById(61L)).thenReturn(Optional.of(target));
        when(userRepository.findByIdForUpdate(61L)).thenReturn(Optional.of(target));
        when(userRepository.saveAndFlush(target)).thenReturn(target);

        var response = service.setActive(61L, false);

        assertThat(response.active()).isFalse();
        verify(refreshTokenService).revokeAllForUser(61L);
        verify(identityCacheInvalidator).invalidateDashboardAfterCommit(61L);
    }

    @Test
    void cannotDeactivateSelfOrFinalActiveAdministrator() {
        DashboardUserEntity actor = user(1L, DashboardRole.ADMIN, true);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(actor);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> service.setActive(1L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own account");

        DashboardUserEntity finalAdmin = user(2L, DashboardRole.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(finalAdmin));
        when(userRepository.lockActiveAdministrators()).thenReturn(List.of(finalAdmin));

        assertThatThrownBy(() -> service.setActive(2L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("final active ADMIN");
        verify(refreshTokenService, never()).revokeAllForUser(2L);
    }

    @Test
    void passwordResetHashesNewCredentialRevokesRefreshAndInvalidatesIdentity() {
        DashboardUserEntity target = contentStaff(71L, true);
        target.setPasswordHash(passwordEncoder.encode("Old!Password123"));
        when(userRepository.findByIdForUpdate(71L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        service.resetPassword(71L, "New!Password456");

        assertThat(passwordEncoder.matches("Old!Password123", target.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("New!Password456", target.getPasswordHash())).isTrue();
        verify(refreshTokenService).revokeAllForUser(71L);
        verify(identityCacheInvalidator).invalidateDashboardAfterCommit(71L);
    }

    private DashboardUserEntity contentStaff(Long id, boolean active) {
        return user(id, DashboardRole.CONTENT_STAFF, active);
    }

    private DashboardUserEntity user(Long id, DashboardRole role, boolean active) {
        return DashboardUserEntity.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .name("User " + id)
                .passwordHash("hash")
                .role(role)
                .active(active)
                .permissions(new LinkedHashSet<DashboardPermission>())
                .build();
    }
}
