package com.brandPitara.sfs.marketplace.enums;

/**
 * Trust state of a store's recommendation of a worker. Only a dashboard ADMIN/REVIEWER can set
 * VERIFIED or REJECTED; only VERIFIED is ever shown publicly.
 */
public enum WorkerRecommendationStatus {
    NONE,
    PENDING,
    VERIFIED,
    REJECTED
}
