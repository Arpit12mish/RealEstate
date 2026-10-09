package com.brandPitara.sfs.marketplace.dto;

import java.math.BigDecimal;

/** Aggregate of APPROVED reviews only; maintained server-side, never client supplied. */
public record RatingSummaryResponse(BigDecimal average, int count) {
}
