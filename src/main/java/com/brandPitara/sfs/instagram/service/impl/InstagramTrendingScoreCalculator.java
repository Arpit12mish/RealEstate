package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Pure scoring formula, extracted out of InstagramReelSyncServiceImpl so it
 * can be shared with InstagramTrendingScoreRecalculator without either class
 * depending on the other.
 */
@Component
public class InstagramTrendingScoreCalculator {

    public BigDecimal calculate(InstagramReelEntity entity) {
        double score =
            nonNull(entity.getViewCount()) * 0.50
                + nonNull(entity.getLikeCount()) * 0.20
                + nonNull(entity.getCommentCount()) * 0.20
                + nonNull(entity.getShareCount()) * 0.10
                + nonNull(entity.getSaveCount()) * 0.10;

        OffsetDateTime publishedAt = entity.getPublishedAt();
        if (publishedAt != null) {
            long ageHours = ChronoUnit.HOURS.between(publishedAt, OffsetDateTime.now());
            if (ageHours <= 24) {
                score += 500;
            } else if (ageHours <= 24 * 7) {
                score += 250;
            } else if (ageHours <= 24 * 30) {
                score += 100;
            }
        }

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private Long nonNull(Long value) {
        return value != null ? value : 0L;
    }
}
