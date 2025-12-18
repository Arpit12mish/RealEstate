package com.brandPitara.sfs.scheduler;

import com.brandPitara.sfs.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    // Run daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanup() {

        int removed = refreshTokenRepository.deleteExpiredTokens(OffsetDateTime.now());

        log.info("🧹 Refresh Token Cleanup: removed {} expired refresh tokens", removed);
    }
}
