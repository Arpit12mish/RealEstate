package com.brandPitara.sfs.dashboard.promobanner.dto;

import com.brandPitara.sfs.enums.PromoBannerMediaType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DashboardPromoBannerUpsertRequest {

    @NotNull
    @Min(0)
    private Long categoryId;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "slotKey may contain only letters, numbers, underscores, and hyphens")
    private String slotKey;

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 255)
    private String subtitle;

    @NotNull
    private PromoBannerMediaType mediaType;

    @NotBlank
    @Size(max = 2048)
    @Pattern(regexp = "^https?://.+", message = "mediaUrl must be a permanent http(s) URL")
    private String mediaUrl;

    @Size(max = 500)
    private String targetUrl;

    @Min(0)
    private Integer priority;

    private Boolean active;

    @Min(1000)
    @Max(60000)
    private Integer displayDurationMs;

    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
}
