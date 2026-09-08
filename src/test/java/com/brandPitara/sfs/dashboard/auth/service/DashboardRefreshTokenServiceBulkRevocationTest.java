package com.brandPitara.sfs.dashboard.auth.service;

import com.brandPitara.sfs.dashboard.auth.repository.DashboardRefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardRefreshTokenServiceBulkRevocationTest {

    @Test
    void revokesEveryActiveRefreshTokenForDashboardUser() {
        DashboardRefreshTokenRepository repository = mock(DashboardRefreshTokenRepository.class);
        when(repository.revokeAllActiveByDashboardUserId(org.mockito.ArgumentMatchers.eq(91L),
                org.mockito.ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(3);
        DashboardRefreshTokenService service = new DashboardRefreshTokenService(repository);

        int revoked = service.revokeAllForUser(91L);

        ArgumentCaptor<OffsetDateTime> revokedAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).revokeAllActiveByDashboardUserId(org.mockito.ArgumentMatchers.eq(91L), revokedAt.capture());
        assertThat(revoked).isEqualTo(3);
        assertThat(revokedAt.getValue()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
