package com.brandPitara.sfs.projectcompare.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComparisonCellValue {
    /** Human-readable value shown in the UI cell (e.g. "₹2.05 Cr – ₹6.10 Cr", "8/10", "YES", "—"). */
    private final String displayValue;

    /** Raw machine-readable value for sorting / future logic; null when unavailable. */
    private final Object rawValue;

    /** False when the project has no data for this row. */
    private final boolean available;

    public static ComparisonCellValue missing() {
        return ComparisonCellValue.builder()
                .displayValue("—")
                .rawValue(null)
                .available(false)
                .build();
    }

    public static ComparisonCellValue of(String display, Object raw) {
        return ComparisonCellValue.builder()
                .displayValue(display)
                .rawValue(raw)
                .available(true)
                .build();
    }
}
