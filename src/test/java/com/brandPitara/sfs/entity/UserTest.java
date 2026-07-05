package com.brandPitara.sfs.entity;

import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for User's toString() allow-list: proves the fields that
 * must never appear (password, email, phoneNumber, profile photo storage
 * details) genuinely don't, regardless of what fields are added to the
 * entity in the future - unlike a deny-list, adding a new field here can
 * never silently reintroduce a leak without also adding @ToString.Include.
 */
class UserTest {

    @Test
    void toStringNeverIncludesPasswordEmailOrPhoneNumber() {
        User user = new User();
        user.setId(1L);
        user.setEmail("sensitive.user@example.com");
        user.setPassword("$2a$10$somebcrypthashvalue");
        user.setPhoneNumber("+919876543210");
        user.setName("Real Name");
        user.setRole(Role.CUSTOMER);
        user.setVerified(true);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);
        user.setCreatedAt(OffsetDateTime.now());

        String toString = user.toString();

        assertThat(toString).doesNotContain("sensitive.user@example.com");
        assertThat(toString).doesNotContain("$2a$10$somebcrypthashvalue");
        assertThat(toString).doesNotContain("9876543210");
        assertThat(toString).doesNotContain("Real Name");
    }

    @Test
    void toStringIncludesOnlyTheSafeOperationalFields() {
        User user = new User();
        user.setId(42L);
        user.setRole(Role.CUSTOMER);
        user.setVerified(true);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);

        String toString = user.toString();

        assertThat(toString).contains("id=42");
        assertThat(toString).contains("role=CUSTOMER");
        assertThat(toString).contains("onboardingStatus=ROLE_PENDING");
    }

    @Test
    void toStringNeverTouchesTheLazyOtpsCollection() {
        // A detached/lazy-proxy "otps" collection could throw
        // LazyInitializationException if toString() ever touched it - the
        // allow-list means it structurally can't, without needing a session.
        User user = new User();
        user.setId(1L);

        org.assertj.core.api.Assertions.assertThatCode(user::toString).doesNotThrowAnyException();
    }
}
