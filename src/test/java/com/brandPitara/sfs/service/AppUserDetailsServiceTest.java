package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * Verifies that JWT subjects issued before the V107 phone-canonicalization migration
 * (raw 10-digit, 0-prefixed, 91-prefixed) still resolve to the canonical +91 user
 * after that migration rewrites users.phone_number, so pre-existing sessions survive
 * the deploy instead of getting a mass 401.
 */
@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private AppUserDetailsService service;

    private User canonicalUser() {
        User user = new User();
        user.setId(42L);
        user.setPhoneNumber("+919876543210");
        user.setPassword("encoded");
        user.setVerified(true);
        user.setRole(Role.CUSTOMER);
        return user;
    }

    @Test
    void legacyRawTenDigitJwtSubjectResolvesToCanonicalUser() {
        service = new AppUserDetailsService(userRepository, new UserPhoneLookupService(userRepository));
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(canonicalUser()));

        UserDetails userDetails = service.loadUserByUsername("9876543210");

        assertThat(userDetails.getUsername()).isEqualTo("+919876543210");
    }

    @Test
    void legacyNinetyOnePrefixedJwtSubjectResolvesToCanonicalUser() {
        service = new AppUserDetailsService(userRepository, new UserPhoneLookupService(userRepository));
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(canonicalUser()));

        UserDetails userDetails = service.loadUserByUsername("919876543210");

        assertThat(userDetails.getUsername()).isEqualTo("+919876543210");
    }

    @Test
    void legacyZeroPrefixedJwtSubjectResolvesToCanonicalUser() {
        service = new AppUserDetailsService(userRepository, new UserPhoneLookupService(userRepository));
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(canonicalUser()));

        UserDetails userDetails = service.loadUserByUsername("09876543210");

        assertThat(userDetails.getUsername()).isEqualTo("+919876543210");
    }

    @Test
    void canonicalJwtSubjectStillResolves() {
        service = new AppUserDetailsService(userRepository, new UserPhoneLookupService(userRepository));
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(canonicalUser()));

        UserDetails userDetails = service.loadUserByUsername("+919876543210");

        assertThat(userDetails.getUsername()).isEqualTo("+919876543210");
    }

    @Test
    void fallsBackToEmailLookupWhenIdentifierIsNotAPhoneNumber() {
        service = new AppUserDetailsService(userRepository, new UserPhoneLookupService(userRepository));
        User emailUser = canonicalUser();
        emailUser.setEmail("someone@example.com");
        when(userRepository.findByEmail("someone@example.com")).thenReturn(Optional.of(emailUser));

        UserDetails userDetails = service.loadUserByUsername("someone@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("+919876543210");
    }

    @Test
    void unknownIdentifierThrowsUsernameNotFound() {
        service = new AppUserDetailsService(userRepository, new UserPhoneLookupService(userRepository));
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
