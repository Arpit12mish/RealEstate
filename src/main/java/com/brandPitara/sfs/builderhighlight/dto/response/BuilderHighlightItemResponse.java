package com.brandPitara.sfs.builderhighlight.dto.response;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightMediaType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightSourceType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightItemResponse {
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
    private Boolean publicVisible;
    private Boolean active;
    private Integer sortOrder;
    private BuilderHighlightStatus status;
    private Long createdBy;
    private Long updatedBy;
    private Long approvedBy;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
    private List<BuilderHighlightPointResponse> points;
}
