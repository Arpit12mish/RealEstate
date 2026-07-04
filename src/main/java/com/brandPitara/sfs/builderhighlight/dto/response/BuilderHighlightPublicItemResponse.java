package com.brandPitara.sfs.builderhighlight.dto.response;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightMediaType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightSourceType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightPublicItemResponse {
    private Long id;
    private Long builderId;
    private Long projectId;
    private String projectName;
    private Long cityId;
    private String cityName;
    private BuilderHighlightType highlightType;
    private BuilderHighlightSourceType sourceType;
    private BuilderHighlightMediaType mediaType;
    private String title;
    private String subtitle;
    private String summary;
    private String body;
    private String tagLabel;
    private String tagType;
    private String thumbnailUrl;
    private String imageUrl;
    private String videoUrl;
    private String youtubeVideoId;
    private String externalUrl;
    private Boolean webviewEnabled;
    private String publisherName;
    private String authorLabel;
    private Integer readTimeMinutes;
    private OffsetDateTime publishedAt;
    private Boolean featured;
    private Boolean verified;
    private Integer sortOrder;
    private List<BuilderHighlightPublicPointResponse> points;
}
