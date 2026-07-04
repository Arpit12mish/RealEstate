package com.brandPitara.sfs.instagram.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record InstagramMetaMedia(
    String id,
    String caption,
    @JsonProperty("media_type") String mediaType,
    @JsonProperty("media_product_type") String mediaProductType,
    String permalink,
    @JsonProperty("thumbnail_url") String thumbnailUrl,
    OffsetDateTime timestamp,
    @JsonProperty("like_count") Long likeCount,
    @JsonProperty("comments_count") Long commentsCount
) {}
