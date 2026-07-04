package com.brandPitara.sfs.builderhighlight.mapper;

import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightItemResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightPointResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightPublicItemResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightPublicPointResponse;
import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightItemEntity;
import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightPointEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class BuilderHighlightMapper {

    public BuilderHighlightItemResponse toResponse(BuilderHighlightItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return BuilderHighlightItemResponse.builder()
            .id(entity.getId())
            .builderId(entity.getBuilder() != null ? entity.getBuilder().getId() : null)
            .projectId(entity.getProject() != null ? entity.getProject().getId() : null)
            .projectName(entity.getProject() != null ? entity.getProject().getName() : null)
            .cityId(entity.getCity() != null ? entity.getCity().getId() : null)
            .cityName(entity.getCity() != null ? entity.getCity().getName() : null)
            .highlightType(entity.getHighlightType())
            .sourceType(entity.getSourceType())
            .mediaType(entity.getMediaType())
            .title(entity.getTitle())
            .subtitle(entity.getSubtitle())
            .summary(entity.getSummary())
            .body(entity.getBody())
            .tagLabel(entity.getTagLabel())
            .tagType(entity.getTagType())
            .thumbnailUrl(entity.getThumbnailUrl())
            .imageUrl(entity.getImageUrl())
            .videoUrl(entity.getVideoUrl())
            .youtubeVideoId(entity.getYoutubeVideoId())
            .externalUrl(entity.getExternalUrl())
            .webviewEnabled(entity.getWebviewEnabled())
            .publisherName(entity.getPublisherName())
            .authorLabel(entity.getAuthorLabel())
            .readTimeMinutes(entity.getReadTimeMinutes())
            .publishedAt(entity.getPublishedAt())
            .featured(entity.getFeatured())
            .verified(entity.getVerified())
            .publicVisible(entity.getPublicVisible())
            .active(entity.getActive())
            .sortOrder(entity.getSortOrder())
            .status(entity.getStatus())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .approvedBy(entity.getApprovedBy())
            .approvedAt(entity.getApprovedAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .deletedAt(entity.getDeletedAt())
            .points(toPointResponses(entity.getPoints()))
            .build();
    }

    public BuilderHighlightPublicItemResponse toPublicResponse(BuilderHighlightItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return BuilderHighlightPublicItemResponse.builder()
            .id(entity.getId())
            .builderId(entity.getBuilder() != null ? entity.getBuilder().getId() : null)
            .projectId(entity.getProject() != null ? entity.getProject().getId() : null)
            .projectName(entity.getProject() != null ? entity.getProject().getName() : null)
            .cityId(entity.getCity() != null ? entity.getCity().getId() : null)
            .cityName(entity.getCity() != null ? entity.getCity().getName() : null)
            .highlightType(entity.getHighlightType())
            .sourceType(entity.getSourceType())
            .mediaType(entity.getMediaType())
            .title(entity.getTitle())
            .subtitle(entity.getSubtitle())
            .summary(entity.getSummary())
            .body(entity.getBody())
            .tagLabel(entity.getTagLabel())
            .tagType(entity.getTagType())
            .thumbnailUrl(entity.getThumbnailUrl())
            .imageUrl(entity.getImageUrl())
            .videoUrl(entity.getVideoUrl())
            .youtubeVideoId(entity.getYoutubeVideoId())
            .externalUrl(entity.getExternalUrl())
            .webviewEnabled(entity.getWebviewEnabled())
            .publisherName(entity.getPublisherName())
            .authorLabel(entity.getAuthorLabel())
            .readTimeMinutes(entity.getReadTimeMinutes())
            .publishedAt(entity.getPublishedAt())
            .featured(entity.getFeatured())
            .verified(entity.getVerified())
            .sortOrder(entity.getSortOrder())
            .points(toPublicPointResponses(entity.getPoints()))
            .build();
    }

    public BuilderHighlightPointResponse toPointResponse(BuilderHighlightPointEntity entity) {
        if (entity == null) {
            return null;
        }

        return BuilderHighlightPointResponse.builder()
            .id(entity.getId())
            .pointType(entity.getPointType())
            .title(entity.getTitle())
            .text(entity.getText())
            .iconKey(entity.getIconKey())
            .displayOrder(entity.getDisplayOrder())
            .active(entity.getActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private List<BuilderHighlightPointResponse> toPointResponses(List<BuilderHighlightPointEntity> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        return points.stream()
            .filter(point -> Boolean.TRUE.equals(point.getActive()))
            .sorted(Comparator
                .comparing(BuilderHighlightPointEntity::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BuilderHighlightPointEntity::getId, Comparator.nullsLast(Long::compareTo)))
            .map(this::toPointResponse)
            .toList();
    }

    private List<BuilderHighlightPublicPointResponse> toPublicPointResponses(List<BuilderHighlightPointEntity> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        return points.stream()
            .filter(point -> Boolean.TRUE.equals(point.getActive()))
            .sorted(Comparator
                .comparing(BuilderHighlightPointEntity::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BuilderHighlightPointEntity::getId, Comparator.nullsLast(Long::compareTo)))
            .map(point -> BuilderHighlightPublicPointResponse.builder()
                .id(point.getId())
                .pointType(point.getPointType())
                .title(point.getTitle())
                .text(point.getText())
                .iconKey(point.getIconKey())
                .displayOrder(point.getDisplayOrder())
                .active(point.getActive())
                .build())
            .toList();
    }
}
