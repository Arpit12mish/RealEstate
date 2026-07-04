package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterCardResponse {
    private Long projectId;
    private String projectName;
    private String projectSlug;

    private Long builderId;
    private String builderName;
    private String builderLogoUrl;

    private String coverImageUrl;

    private String addressLine;
    private String cityName;

    private Long priceMin;
    private Long priceMax;

    private Integer constructionProgressPercent;
    private Double appreciationPercent;
    private LocalDate projectStartDate;
    private LocalDate startedOn;

    private String timelineStatus;
    private String timelineLabel;
    private String timelineHint;
    private ProjectTimelineResponse timeline;
    private Integer delayDays;
    private boolean delayed;

    @Builder.Default
    private Long favoriteCount = 0L;
    @Builder.Default
    private Boolean isFavorite = false;

    private OffsetDateTime lastUpdatedAt;
}
