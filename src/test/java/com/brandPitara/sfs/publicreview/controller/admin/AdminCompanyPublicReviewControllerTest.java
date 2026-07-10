package com.brandPitara.sfs.publicreview.controller.admin;

import com.brandPitara.sfs.publicreview.dto.SyncGooglePublicReviewsResponse;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCompanyPublicReviewControllerTest {

    @Mock private PublicReviewService publicReviewService;

    private AdminCompanyPublicReviewController controller() {
        return new AdminCompanyPublicReviewController(publicReviewService);
    }

    @Test
    void syncGoogle_mapsGoogleFetchFailureTo502NotRaw500() {
        // GooglePlacesClient throws IllegalStateException for any non-2xx upstream
        // response (bad place ID, quota, outage) - this must not surface as a bare
        // 500 for company syncs, per the "no 500 for a Google fetch failure" rule.
        when(publicReviewService.syncGoogleReviews(PublicReviewTargetType.COMPANY, 7L, 50L))
            .thenThrow(new IllegalStateException("Google Places API failed. status=404"));

        assertThatThrownBy(() -> controller().syncGoogle(7L, 50L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            });
    }

    @Test
    void syncGoogle_returnsResponseOnSuccess() {
        SyncGooglePublicReviewsResponse expected = SyncGooglePublicReviewsResponse.builder()
            .reviewPlaceId(50L)
            .googlePlaceId("place-123")
            .rating(java.math.BigDecimal.valueOf(4.5))
            .userRatingCount(10)
            .fetchedReviewSampleCount(5)
            .syncedAt(OffsetDateTime.now())
            .build();
        when(publicReviewService.syncGoogleReviews(PublicReviewTargetType.COMPANY, 7L, 50L))
            .thenReturn(expected);

        SyncGooglePublicReviewsResponse actual = controller().syncGoogle(7L, 50L);

        assertThat(actual.getGooglePlaceId()).isEqualTo("place-123");
        assertThat(actual.getFetchedReviewSampleCount()).isEqualTo(5);
    }
}
