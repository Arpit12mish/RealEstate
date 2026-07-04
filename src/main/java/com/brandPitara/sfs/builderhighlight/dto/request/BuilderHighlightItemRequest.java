package com.brandPitara.sfs.builderhighlight.dto.request;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightMediaType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightSourceType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderHighlightItemRequest {

    private Long projectId;
    private Long cityId;

    @NotNull
    private BuilderHighlightType highlightType;

    @NotNull
    private BuilderHighlightSourceType sourceType;

    @NotNull
    private BuilderHighlightMediaType mediaType;

    @NotBlank
    @Size(max = 180)
    private String title;

    @Size(max = 255)
    private String subtitle;

    @Size(max = 2000)
    private String summary;

    @Size(max = 10000)
    private String body;

    @Size(max = 80)
    private String tagLabel;

    @Size(max = 60)
    private String tagType;

    @Size(max = 1000)
    private String thumbnailUrl;

    @Size(max = 1000)
    private String imageUrl;

    @Size(max = 1000)
    private String videoUrl;

    @Size(max = 80)
    private String youtubeVideoId;

    @Size(max = 1000)
    private String externalUrl;

    private Boolean webviewEnabled;

    @Size(max = 180)
    private String publisherName;

    @Size(max = 150)
    private String authorLabel;

    @Min(0)
    private Integer readTimeMinutes;

    private OffsetDateTime publishedAt;
    private Boolean featured;
    private Boolean verified;
    private Boolean publicVisible;
    private Boolean active;

    @Min(0)
    private Integer sortOrder;

    private BuilderHighlightStatus status;

    @Valid
    @Builder.Default
    private List<BuilderHighlightPointRequest> points = new ArrayList<>();
}
