package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A separate bean - not a method on InstagramReelSyncServiceImpl - so that
 * calling it always goes through Spring's real proxy and @Transactional
 * actually applies. InstagramReelSyncServiceImpl previously called this same
 * logic on itself (`this.recalculateTrendingScores()`), which Spring's
 * proxy-based AOP cannot intercept: @Transactional silently did nothing on
 * that internal call, so the read (findByDeletedFalse) and the write
 * (saveAll) ran as two uncoordinated operations instead of one atomic unit.
 */
@Service
@RequiredArgsConstructor
public class InstagramTrendingScoreRecalculator {

    private final InstagramReelRepository instagramReelRepository;
    private final InstagramTrendingScoreCalculator trendingScoreCalculator;

    @Transactional
    public void recalculateTrendingScores() {
        List<InstagramReelEntity> reels = instagramReelRepository.findByDeletedFalse();
        reels.forEach(entity -> entity.setTrendingScore(trendingScoreCalculator.calculate(entity)));
        instagramReelRepository.saveAll(reels);
    }
}
