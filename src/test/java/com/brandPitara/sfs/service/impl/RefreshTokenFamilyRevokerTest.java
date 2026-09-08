package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenFamilyRevokerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenFamilyRevoker revoker;

    @Test
    void revokesOnlyTheGivenDeviceWhenDeviceIdIsPresent() {
        revoker.revokeFamily(5L, "ios-1");

        verify(refreshTokenRepository).revokeActiveByUserIdAndDeviceId(5L, "ios-1");
        verify(refreshTokenRepository, never()).revokeAllByUserId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void revokesEveryDeviceWhenDeviceIdIsAbsent() {
        revoker.revokeFamily(5L, null);

        verify(refreshTokenRepository).revokeAllByUserId(5L);
        verify(refreshTokenRepository, never())
                .revokeActiveByUserIdAndDeviceId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void revokesEveryDeviceWhenDeviceIdIsBlank() {
        revoker.revokeFamily(5L, "   ");

        verify(refreshTokenRepository).revokeAllByUserId(5L);
    }
}
