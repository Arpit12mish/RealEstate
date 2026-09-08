package com.brandPitara.sfs.cms.content.document;

import com.brandPitara.sfs.cms.content.dto.ContentDocumentMediaResponse;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import com.brandPitara.sfs.media.service.PresignedReadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CmsMediaReferenceService {

    private final CmsMediaAssetRepository repository;
    private final MediaObjectStorageService storage;

    public Map<Long, CmsMediaAssetEntity> validateAndResolve(ContentDocument document) {
        Map<Long, Set<CmsMediaType>> expectedTypes = collectExpectedTypes(document);
        return validateAndResolve(expectedTypes);
    }

    public Map<Long, CmsMediaAssetEntity> validateAndResolve(
            ContentDocument document, Map<Long, CmsMediaType> additionalReferences
    ) {
        Map<Long, Set<CmsMediaType>> expectedTypes = collectExpectedTypes(document);
        if (additionalReferences != null) {
            additionalReferences.forEach((id, type) -> {
                if (id != null) add(expectedTypes, id, type);
            });
        }
        return validateAndResolve(expectedTypes);
    }

    private Map<Long, CmsMediaAssetEntity> validateAndResolve(Map<Long, Set<CmsMediaType>> expectedTypes) {
        if (expectedTypes.isEmpty()) {
            return Map.of();
        }

        List<CmsMediaAssetEntity> found = repository.findAllByIdIn(expectedTypes.keySet());
        Map<Long, CmsMediaAssetEntity> assets = new LinkedHashMap<>();
        found.forEach(asset -> assets.put(asset.getId(), asset));
        for (Map.Entry<Long, Set<CmsMediaType>> reference : expectedTypes.entrySet()) {
            CmsMediaAssetEntity asset = assets.get(reference.getKey());
            if (asset == null) {
                throw CmsContentApiException.mediaNotFound(reference.getKey());
            }
            if (asset.getStatus() != CmsMediaStatus.READY) {
                throw CmsContentApiException.mediaNotReady(reference.getKey());
            }
            if (reference.getValue().size() != 1 || !reference.getValue().contains(asset.getMediaType())) {
                String expected = reference.getValue().stream()
                        .map(Enum::name)
                        .sorted()
                        .reduce((left, right) -> left + " or " + right)
                        .orElse("a supported media type");
                throw CmsContentApiException.mediaTypeMismatch(reference.getKey(), expected);
            }
        }
        return Map.copyOf(assets);
    }

    public Map<Long, ContentDocumentMediaResponse> createPreviewMap(
            Map<Long, CmsMediaAssetEntity> assets
    ) {
        if (assets.isEmpty()) {
            return Map.of();
        }
        Map<Long, ContentDocumentMediaResponse> responses = new LinkedHashMap<>();
        assets.values().stream()
                .sorted(java.util.Comparator.comparing(CmsMediaAssetEntity::getId))
                .forEach(asset -> responses.put(asset.getId(), responseWithBestEffortPreview(asset)));
        return Map.copyOf(responses);
    }

    private ContentDocumentMediaResponse responseWithBestEffortPreview(CmsMediaAssetEntity asset) {
        String previewUrl = null;
        Integer previewExpiry = null;
        try {
            PresignedReadResult preview = storage.createPresignedRead(
                    asset.getStorageBucket(), asset.getStorageKey()
            );
            previewUrl = preview.url();
            previewExpiry = preview.expiresInSeconds();
        } catch (RuntimeException ignored) {
            // Preview signing must not make an otherwise valid document read or autosave fail.
        }
        return new ContentDocumentMediaResponse(
                asset.getId(), asset.getMediaType().name(), asset.getOriginalFilename(),
                asset.getContentType(), asset.getSizeBytes(), asset.getWidth(), asset.getHeight(),
                asset.getDurationMillis(), previewUrl, previewExpiry
        );
    }

    private Map<Long, Set<CmsMediaType>> collectExpectedTypes(ContentDocument document) {
        Map<Long, Set<CmsMediaType>> expected = new LinkedHashMap<>();
        if (document == null || document.blocks() == null) return expected;
        for (ContentBlock block : document.blocks()) {
            collectFromBlock(block, expected);
        }
        return expected;
    }

    private void collectFromBlock(ContentBlock block, Map<Long, Set<CmsMediaType>> expected) {
        if (block instanceof ContentBlock.Image image) {
            add(expected, image.mediaAssetId(), CmsMediaType.IMAGE);
        } else if (block instanceof ContentBlock.Video video) {
            add(expected, video.mediaAssetId(), CmsMediaType.VIDEO);
            if (video.posterMediaAssetId() != null) {
                add(expected, video.posterMediaAssetId(), CmsMediaType.IMAGE);
            }
        } else if (block instanceof ContentBlock.Layout layout && layout.children() != null) {
            // LAYOUT children are IMAGE/TABLE only (LayoutChildBlock) - only IMAGE ever
            // contributes a media reference, but this dispatches through the same
            // collectFromBlock path (each child also implements ContentBlock) rather than
            // duplicating the IMAGE branch, so a future widening of LayoutChildBlock (e.g. to
            // VIDEO) picks up media resolution automatically.
            for (LayoutChildBlock child : layout.children()) {
                if (child instanceof ContentBlock contentBlock) {
                    collectFromBlock(contentBlock, expected);
                }
            }
        }
    }

    private void add(Map<Long, Set<CmsMediaType>> expected, Long id, CmsMediaType type) {
        expected.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(type);
    }
}
