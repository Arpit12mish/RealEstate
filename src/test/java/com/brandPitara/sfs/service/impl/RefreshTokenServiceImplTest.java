package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.RefreshTokenRepository;
import com.brandPitara.sfs.service.model.RefreshTokenRotationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(refreshTokenRepository);
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 30L);
    }

    @Test
    void rotateRefreshTokenRevokesSubmittedTokenAndCreatesReplacement() {
        User user = user(5L);
        RefreshToken oldToken = refreshToken(user, false, OffsetDateTime.now().plusDays(1), "ios-1");
        when(refreshTokenRepository.findByTokenForUpdate(anyString())).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenRotationResult result = service.rotateRefreshToken("raw-token");

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getDeviceId()).isEqualTo("ios-1");
        assertThat(result.getRefreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<RefreshToken> saved = captor.getAllValues();
        assertThat(saved.get(0).isRevoked()).isTrue();
        assertThat(saved.get(1).isRevoked()).isFalse();
        assertThat(saved.get(1).getToken()).isNotEqualTo(oldToken.getToken());
        assertThat(saved.get(1).getUser()).isSameAs(user);
        assertThat(saved.get(1).getDeviceId()).isEqualTo("ios-1");
    }

    @Test
    void reusedRevokedRefreshTokenRevokesSameDeviceSessionAndDoesNotCreateReplacement() {
        User user = user(5L);
        RefreshToken oldToken = refreshToken(user, true, OffsetDateTime.now().plusDays(1), "ios-1");
        when(refreshTokenRepository.findByTokenForUpdate(anyString())).thenReturn(Optional.of(oldToken));

        assertThatThrownBy(() -> service.rotateRefreshToken("raw-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reuse");

        verify(refreshTokenRepository).revokeActiveByUserIdAndDeviceId(5L, "ios-1");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void reusedRevokedRefreshTokenWithoutDeviceRevokesAllUserTokens() {
        User user = user(5L);
        RefreshToken oldToken = refreshToken(user, true, OffsetDateTime.now().plusDays(1), null);
        when(refreshTokenRepository.findByTokenForUpdate(anyString())).thenReturn(Optional.of(oldToken));

        assertThatThrownBy(() -> service.rotateRefreshToken("raw-token"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(refreshTokenRepository).revokeAllByUserId(5L);
    }

    @Test
    void expiredRefreshTokenIsRevokedAndRejected() {
        User user = user(5L);
        RefreshToken oldToken = refreshToken(user, false, OffsetDateTime.now().minusDays(1), "ios-1");
        when(refreshTokenRepository.findByTokenForUpdate(anyString())).thenReturn(Optional.of(oldToken));

        assertThatThrownBy(() -> service.rotateRefreshToken("raw-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        assertThat(oldToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(oldToken);
    }

    @Test
    void verifyForLogoutOnlyReturnsTokenWithoutRotatingOrLockingIt() {
        RefreshToken token = refreshToken(user(5L), false, OffsetDateTime.now().plusDays(1), "ios-1");
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(token));

        RefreshToken result = service.verifyForLogoutOnly("raw-token");

        assertThat(result).isSameAs(token);
        verify(refreshTokenRepository, never()).findByTokenForUpdate(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void verifyForLogoutOnlyRejectsRevokedToken() {
        RefreshToken token = refreshToken(user(5L), true, OffsetDateTime.now().plusDays(1), "ios-1");
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyForLogoutOnly("raw-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyForLogoutOnlyRejectsUnknownToken() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyForLogoutOnly("raw-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logoutRevokesStoredRefreshTokenHash() {
        RefreshToken token = refreshToken(user(5L), false, OffsetDateTime.now().plusDays(1), "ios-1");
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(token));

        service.revokeToken("raw-token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setPhoneNumber("+919876543210");
        return user;
    }

    private RefreshToken refreshToken(User user, boolean revoked, OffsetDateTime expiresAt, String deviceId) {
        return RefreshToken.builder()
                .id(10L)
                .user(user)
                .token("old-hash")
                .deviceId(deviceId)
                .fcmToken("fcm")
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
    }
}
