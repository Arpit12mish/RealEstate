package com.brandPitara.sfs.instagram.dto;

import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardInstagramReelUpsertRequest {

    @Size(max = 180)
    private String title;

    private String caption;

    @NotBlank
    @Size(max = 500)
    @Pattern(
        regexp = "^https://(www\\.)?instagram\\.com/reel/.+",
        message = "instagramUrl must be an Instagram reel permalink"
    )
    private String instagramUrl;

    @Size(max = 1000)
    private String thumbnailUrl;

    @Size(max = 1000)
    private String previewVideoUrl;

    private InstagramReelCategory categoryOverride;

    private Integer displayOrder;

    private Boolean active;
}
