package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.model.UserLoginResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void upgradesLegacyIndianPhoneToCanonicalInsteadOfCreatingDuplicate() {
        User legacyUser = new User();
        legacyUser.setId(7L);
        legacyUser.setPhoneNumber("9876543210");
        legacyUser.setRole(Role.CUSTOMER);

        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(legacyUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserLoginResult result = userService.findOrCreateVerifiedUserByPhone("+91 98765 43210");

        assertThat(result.isNewUser()).isFalse();
        assertThat(result.getUser().getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(result.getUser().isVerified()).isTrue();
        verify(userRepository).save(legacyUser);
    }

    @Test
    void createsNewUserWithCanonicalPhoneNumber() {
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(11L);
            return user;
        });

        UserLoginResult result = userService.findOrCreateVerifiedUserByPhone("9876543210");

        assertThat(result.isNewUser()).isTrue();
        assertThat(result.getUser().getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(result.getUser().getEmail()).isEqualTo("phone_919876543210@phone.local");
        assertThat(result.getUser().getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void lookupIncludesCanonicalAndLegacyPhoneVariants() {
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.findOrCreateVerifiedUserByPhone("9876543210");

        ArgumentCaptor<java.util.Collection<String>> lookupCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(userRepository).findByPhoneNumberIn(lookupCaptor.capture());
        assertThat(lookupCaptor.getValue())
                .contains("+919876543210", "9876543210", "919876543210", "09876543210");
    }

    @Test
    void multipleLegacyUsersForSameCanonicalPhoneFailClosed() {
        User legacyUser = new User();
        legacyUser.setId(7L);
        legacyUser.setPhoneNumber("9876543210");

        User canonicalUser = new User();
        canonicalUser.setId(8L);
        canonicalUser.setPhoneNumber("+919876543210");

        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(legacyUser, canonicalUser));

        assertThatThrownBy(() -> userService.findOrCreateVerifiedUserByPhone("919876543210"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Multiple accounts found");

        verify(userRepository, never()).save(any(User.class));
    }
}
