package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.entity.BusinessEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DealerCardBadgeTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    private static BusinessEntity business(int daysOld, double avg, int total) {
        BusinessEntity b = new BusinessEntity();
        b.setCreatedAt(OffsetDateTime.ofInstant(NOW.minus(daysOld, ChronoUnit.DAYS), ZoneOffset.UTC));
        b.setAvgRating(avg);
        b.setTotalRatings(total);
        return b;
    }

    @Test
    void ratingIsHiddenUntilThereIsAReview() {
        assertThat(DealerCardAssembler.averageRating(business(100, 0.0, 0))).isNull();
        assertThat(DealerCardAssembler.averageRating(business(100, 4.64, 38))).isEqualTo(4.6);
        assertThat(DealerCardAssembler.reviewCount(business(100, 4.6, 38))).isEqualTo(38);
    }

    @Test
    void recentListingIsNewlyOnboardedEvenWhenWellRated() {
        assertThat(DealerCardAssembler.badge(business(5, 4.9, 40), NOW)).isEqualTo("Newly onboarded");
    }

    @Test
    void olderWellReviewedStoreIsTopRated() {
        assertThat(DealerCardAssembler.badge(business(90, 4.6, 12), NOW)).isEqualTo("Top rated");
    }

    @Test
    void otherwiseNoBadge() {
        assertThat(DealerCardAssembler.badge(business(90, 4.6, 3), NOW)).isNull();
        assertThat(DealerCardAssembler.badge(business(90, 3.9, 50), NOW)).isNull();
    }
}
