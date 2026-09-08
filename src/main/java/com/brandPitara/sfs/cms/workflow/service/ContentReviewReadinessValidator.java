package com.brandPitara.sfs.cms.workflow.service;

import com.brandPitara.sfs.cms.content.document.*;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ContentReviewReadinessValidator {

    private final CmsMediaReferenceService mediaReferenceService;

    public void validate(ContentPostEntity post) {
        ContentDocument document = post.getContentDocument();
        if (document == null || document.blocks() == null || document.blocks().stream().noneMatch(this::meaningful)) {
            throw CmsContentApiException.notReviewReady(
                    "Content document must contain meaningful text or media before review."
            );
        }
        if (post.getPublicAuthor() == null) {
            throw CmsContentApiException.notReviewReady("A public author is required before review.");
        }
        if (post.getCategory() == null) {
            throw CmsContentApiException.notReviewReady("A category is required before review.");
        }
        if (post.getCoverMediaAsset() == null || post.getCoverAltText() == null || post.getCoverAltText().isBlank()) {
            throw CmsContentApiException.notReviewReady("A READY image cover and meaningful cover alt text are required before review.");
        }
        if (post.getReadingTimeMinutes() == null) {
            throw CmsContentApiException.notReviewReady("Reading time is required before submitting for review.");
        }
        if (!Boolean.TRUE.equals(post.getPublicAuthor().getActive()) || !Boolean.TRUE.equals(post.getCategory().getActive())
                || post.getTags().stream().anyMatch(tag -> !Boolean.TRUE.equals(tag.getActive()))) {
            throw CmsContentApiException.notReviewReady("Author, category, and assigned tags must be active.");
        }
        Map<Long, CmsMediaType> additional = new LinkedHashMap<>();
        additional.put(post.getCoverMediaAsset().getId(), CmsMediaType.IMAGE);
        if (post.getPublicAuthor().getProfileMediaAsset() != null) {
            additional.put(post.getPublicAuthor().getProfileMediaAsset().getId(), CmsMediaType.IMAGE);
        }
        mediaReferenceService.validateAndResolve(document, additional);
    }

    private boolean meaningful(ContentBlock block) {
        if (block instanceof ContentBlock.Image || block instanceof ContentBlock.Video
                || block instanceof ContentBlock.Embed) return true;
        if (block instanceof ContentBlock.Paragraph paragraph) return visible(paragraph.content());
        if (block instanceof ContentBlock.Heading heading) return visible(heading.content());
        if (block instanceof ContentBlock.Blockquote quote) return visible(quote.content());
        if (block instanceof ContentBlock.BulletList list) {
            return list.items() != null && list.items().stream().anyMatch(item -> visible(item.content()));
        }
        if (block instanceof ContentBlock.OrderedList list) {
            return list.items() != null && list.items().stream().anyMatch(item -> visible(item.content()));
        }
        if (block instanceof ContentBlock.CheckList checkList) {
            return checkList.items() != null && checkList.items().stream().anyMatch(item -> visible(item.content()));
        }
        if (block instanceof ContentBlock.Callout callout) {
            return visible(callout.content());
        }
        if (block instanceof ContentBlock.Table table) {
            return table.rows() != null && table.rows().stream()
                    .flatMap(row -> row.cells() == null
                            ? java.util.stream.Stream.<java.util.List<InlineNode>>empty()
                            : row.cells().stream())
                    .anyMatch(this::visible);
        }
        if (block instanceof ContentBlock.Layout layout) {
            return layout.children() != null && layout.children().stream()
                    .anyMatch(child -> child instanceof ContentBlock contentBlock && meaningful(contentBlock));
        }
        return false;
    }

    private boolean visible(java.util.List<InlineNode> nodes) {
        return nodes != null && nodes.stream()
                .filter(InlineNode.Text.class::isInstance)
                .map(InlineNode.Text.class::cast)
                .map(InlineNode.Text::text)
                .anyMatch(text -> text != null && !text.isBlank());
    }
}
