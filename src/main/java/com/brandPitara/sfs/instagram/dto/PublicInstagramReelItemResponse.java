package com.brandPitara.sfs.instagram.dto;

import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicInstagramReelItemResponse {
    private Long id;
    private String title;
    private String captionPreview;
    private String thumbnailUrl;
    private String previewVideoUrl;
    private String instagramUrl;
    private InstagramReelCategory category;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private OffsetDateTime publishedAt;
}
