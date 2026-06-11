package com.brandPitara.sfs.publicreview.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlaceDetailsResponse {

    private String id;
    private LocalizedText displayName;
    private String formattedAddress;
    private String googleMapsUri;
    private BigDecimal rating;
    private Integer userRatingCount;
    private List<GoogleReview> reviews;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocalizedText {
        private String text;
        private String languageCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleReview {
        private String name;
        private AuthorAttribution authorAttribution;
        private LocalizedText text;
        private LocalizedText originalText;
        private Integer rating;
        private String relativePublishTimeDescription;
        private OffsetDateTime publishTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorAttribution {
        private String displayName;
        private String uri;
        private String photoUri;
    }
}
