package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class DashboardMobilePreviewMeta {

    private Long projectId;
    private String projectName;
    private ReviewStatus reviewStatus;
    private boolean published;
    private boolean active;
    private String previewMode;
    private OffsetDateTime generatedAt;
    private List<String> warnings;
    private List<String> missingSections;
}
