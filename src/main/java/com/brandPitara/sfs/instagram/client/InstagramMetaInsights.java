package com.brandPitara.sfs.instagram.client;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramMetaInsights {
    @Builder.Default
    private Long viewCount = 0L;
    @Builder.Default
    private Long likeCount = 0L;
    @Builder.Default
    private Long commentCount = 0L;
    @Builder.Default
    private Long shareCount = 0L;
    @Builder.Default
    private Long saveCount = 0L;
}
