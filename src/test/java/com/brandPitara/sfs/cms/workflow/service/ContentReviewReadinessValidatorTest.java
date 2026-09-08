package com.brandPitara.sfs.cms.workflow.service;

import com.brandPitara.sfs.cms.content.document.*;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.domain.*;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ContentReviewReadinessValidatorTest {

    private final CmsMediaReferenceService mediaReferenceService = mock(CmsMediaReferenceService.class);
    private final ContentReviewReadinessValidator validator =
            new ContentReviewReadinessValidator(mediaReferenceService);

    @Test
    void rejectsNullEmptyDividerOnlyAndWhitespaceOnlyDocuments() {
        rejected(null);
        rejected(new ContentDocument(2, List.of()));
        rejected(new ContentDocument(2, List.of(new ContentBlock.Divider())));
        rejected(new ContentDocument(2, List.of(new ContentBlock.Paragraph(List.of(
                new InlineNode.Text("  \n\t", List.of())
        )))));
        verifyNoInteractions(mediaReferenceService);
    }

    @Test
    void rejectsMissingReadingTimeWithASpecificMessage() {
        ContentDocument document = new ContentDocument(2, List.of(new ContentBlock.Paragraph(List.of(
                new InlineNode.Text("A reviewable article", List.of())
        ))));
        ContentPostEntity post = post(document);
        post.setReadingTimeMinutes(null);

        assertThatThrownBy(() -> validator.validate(post))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_NOT_REVIEW_READY");
        assertThatThrownBy(() -> validator.validate(post))
                .hasMessageContaining("Reading time is required before submitting for review.");
    }

    @Test
    void acceptsMeaningfulTextAndRevalidatesAllMediaReferences() {
        ContentDocument document = new ContentDocument(2, List.of(new ContentBlock.Paragraph(List.of(
                new InlineNode.Text("A reviewable article", List.of())
        ))));

        assertThatCode(() -> validator.validate(post(document))).doesNotThrowAnyException();

        verify(mediaReferenceService).validateAndResolve(document, Map.of(90L, CmsMediaType.IMAGE));
    }

    @Test
    void structurallyPresentButTextEmptyNewBlocksAreNotMeaningful() {
        rejected(new ContentDocument(3, List.of(new ContentBlock.CheckList(List.of(
                new ContentBlock.ListItem(List.of(new InlineNode.Text("  ", List.of())))
        )))));
        rejected(new ContentDocument(3, List.of(new ContentBlock.Callout(
                CalloutVariant.INFO, "Title", List.of(new InlineNode.Text("  ", List.of()))
        ))));
        rejected(new ContentDocument(3, List.of(new ContentBlock.Table(
                "Caption",
                List.of(new ContentBlock.TableColumn("A")),
                List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(
                        new InlineNode.Text("  ", List.of())
                ))))
        ))));
        verifyNoInteractions(mediaReferenceService);
    }

    @Test
    void newBlocksWithVisibleTextAreMeaningful() {
        ContentDocument checkList = new ContentDocument(3, List.of(new ContentBlock.CheckList(List.of(
                new ContentBlock.ListItem(List.of(new InlineNode.Text("Smart switches", List.of())))
        ))));
        ContentDocument callout = new ContentDocument(3, List.of(new ContentBlock.Callout(
                CalloutVariant.VERDICT, null, List.of(new InlineNode.Text("Strong pick", List.of()))
        )));
        ContentDocument table = new ContentDocument(3, List.of(new ContentBlock.Table(
                null,
                List.of(new ContentBlock.TableColumn("A")),
                List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(
                        new InlineNode.Text("7", List.of())
                ))))
        )));

        for (ContentDocument document : List.of(checkList, callout, table)) {
            assertThatCode(() -> validator.validate(post(document))).doesNotThrowAnyException();
        }
    }

    @Test
    void layoutIsMeaningfulOnlyWhenAtLeastOneChildIsMeaningful() {
        ContentDocument meaningful = new ContentDocument(4, List.of(new ContentBlock.Layout(2, List.of(
                new ContentBlock.Image(1L, false, "Alpha Tower", List.of(), ImageLayout.STANDARD, null),
                new ContentBlock.Table(
                        null, null, List.of(new ContentBlock.TableColumn("A")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(
                                new InlineNode.Text("  ", List.of())
                        ))))
                )
        ))));
        assertThatCode(() -> validator.validate(post(meaningful))).doesNotThrowAnyException();

        // Both children text-empty (table cell blank) is not meaningful, mirroring a top-level
        // TABLE with no visible text - a layout must not get a looser bar than its own children.
        ContentDocument notMeaningful = new ContentDocument(4, List.of(new ContentBlock.Layout(2, List.of(
                new ContentBlock.Table(
                        null, null, List.of(new ContentBlock.TableColumn("A")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(
                                new InlineNode.Text("  ", List.of())
                        ))))
                ),
                new ContentBlock.Table(
                        null, null, List.of(new ContentBlock.TableColumn("B")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(
                                new InlineNode.Text("\t", List.of())
                        ))))
                )
        ))));
        rejected(notMeaningful);
    }

    @Test
    void mediaBlocksAreMeaningfulButStillRequireTrustedReadyAssets() {
        ContentDocument document = new ContentDocument(2, List.of(new ContentBlock.Image(
                77L, false, "Clubhouse", List.of(), ImageLayout.STANDARD, null
        )));

        assertThatCode(() -> validator.validate(post(document))).doesNotThrowAnyException();

        verify(mediaReferenceService).validateAndResolve(document, Map.of(90L, CmsMediaType.IMAGE));
    }

    private void rejected(ContentDocument document) {
        assertThatThrownBy(() -> validator.validate(post(document)))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_NOT_REVIEW_READY");
    }

    private ContentPostEntity post(ContentDocument document) {
        return ContentPostEntity.builder().contentDocument(document)
                .publicAuthor(CmsPublicAuthorEntity.builder().id(3L).displayName("Author").slug("author").active(true).build())
                .category(CmsContentCategoryEntity.builder().id(4L).name("Guides").slug("guides").active(true).build())
                .coverMediaAsset(CmsMediaAssetEntity.builder().id(90L).mediaType(CmsMediaType.IMAGE)
                        .status(CmsMediaStatus.READY).build())
                .coverAltText("Article cover").readingTimeMinutes(5).build();
    }
}
