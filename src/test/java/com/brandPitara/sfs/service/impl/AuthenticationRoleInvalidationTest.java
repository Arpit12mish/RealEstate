package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingRole;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.mapper.OnboardingRoleMapper;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.security.identity.AuthenticationIdentityCacheInvalidator;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationRoleInvalidationTest {

    @Test
    void onboardingRoleChangeInvalidatesMobileAuthenticationSnapshot() {
        UserRepository users = mock(UserRepository.class);
        ProviderProfileRepository profiles = mock(ProviderProfileRepository.class);
        OnboardingRoleMapper roleMapper = mock(OnboardingRoleMapper.class);
        AuthenticationIdentityCacheInvalidator invalidator = mock(AuthenticationIdentityCacheInvalidator.class);
        User user = new User();
        user.setId(12L);
        user.setPhoneNumber("+919876500012");
        user.setRole(Role.CUSTOMER);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);
        when(users.findById(12L)).thenReturn(Optional.of(user));
        when(profiles.findByUserId(12L)).thenReturn(Optional.empty());
        when(roleMapper.toSystemRole(OnboardingRole.WORKER)).thenReturn(Role.WORKER);
        OnboardingServiceImpl service = new OnboardingServiceImpl(users, profiles, roleMapper, invalidator);

        service.chooseRole(12L, OnboardingRole.WORKER);

        assertThat(user.getRole()).isEqualTo(Role.WORKER);
        verify(invalidator).invalidateMobileAfterCommit(12L);
    }
}
