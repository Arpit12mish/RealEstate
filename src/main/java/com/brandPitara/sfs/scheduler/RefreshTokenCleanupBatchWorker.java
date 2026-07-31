package com.brandPitara.sfs.scheduler;

import com.brandPitara.sfs.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupBatchWorker {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * The scheduler is deliberately non-transactional, so REQUIRED starts and commits
     * one independent short transaction for every batch.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int deleteBatch(OffsetDateTime cutoff, int batchSize) {
        Objects.requireNonNull(cutoff, "cutoff must not be null");
        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalArgumentException("batchSize must be between 1 and 10000");
        }
        return refreshTokenRepository.deleteCleanupBatch(cutoff, batchSize);
    }
}
