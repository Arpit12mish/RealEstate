package com.brandPitara.sfs.security;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.UserPhoneLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * CurrentUserService no longer hand-rolls its own phone normalization; it must resolve
 * the JWT principal through the shared UserPhoneLookupService (PhoneNumberNormalizer),
 * so legacy pre-migration JWT subjects still resolve to the canonical +91 user.
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private CurrentUserService currentUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User canonicalUser() {
        User user = new User();
        user.setId(7L);
        user.setPhoneNumber("+919876543210");
        return user;
    }

    private void authenticateAs(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @Test
    void legacyRawPhonePrincipalResolvesToCanonicalUser() {
        currentUserService = new CurrentUserService(new UserPhoneLookupService(userRepository));
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(canonicalUser()));
        authenticateAs("9876543210");

        User result = currentUserService.requireUser();

        assertThat(result.getPhoneNumber()).isEqualTo("+919876543210");
    }

    @Test
    void canonicalPhonePrincipalResolvesToUser() {
        currentUserService = new CurrentUserService(new UserPhoneLookupService(userRepository));
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of(canonicalUser()));
        authenticateAs("+919876543210");

        User result = currentUserService.requireUser();

        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void unauthenticatedContextThrowsUnauthorized() {
        currentUserService = new CurrentUserService(new UserPhoneLookupService(userRepository));
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> currentUserService.requireUser())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void unresolvableIdentifierThrowsNotFound() {
        currentUserService = new CurrentUserService(new UserPhoneLookupService(userRepository));
        when(userRepository.findByPhoneNumberIn(anyCollection())).thenReturn(List.of());
        authenticateAs("9876543210");

        assertThatThrownBy(() -> currentUserService.requireUser())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}
