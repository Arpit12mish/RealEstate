package com.brandPitara.sfs.appscreencontent.dto;

import com.brandPitara.sfs.appscreencontent.entity.AppScreenContentEntity;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenMediaType;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record AppScreenContentResponse(
        Long id,
        AppScreenKey screenKey,
        AppScreenPlacement placement,
        AppScreenMediaType mediaType,
        String mediaUrl,
        Boolean enabled,
        String backgroundColor,
        Double aspectRatio,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String minAppVersion,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AppScreenContentResponse from(AppScreenContentEntity entity) {
        return AppScreenContentResponse.builder()
                .id(entity.getId())
                .screenKey(entity.getScreenKey())
                .placement(entity.getPlacement())
                .mediaType(entity.getMediaType())
                .mediaUrl(entity.getMediaUrl())
                .enabled(entity.getEnabled())
                .backgroundColor(entity.getBackgroundColor())
                .aspectRatio(entity.getAspectRatio())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .minAppVersion(entity.getMinAppVersion())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
