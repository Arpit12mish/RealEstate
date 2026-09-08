package com.brandPitara.sfs.publicreview.enums;

public enum GoogleReviewFetchStatus {
    NOT_FETCHED,
    /** Reserved by an in-flight sync; a concurrent sync attempt must be rejected, not proceed. */
    FETCHING,
    FETCHED,
    FAILED
}
