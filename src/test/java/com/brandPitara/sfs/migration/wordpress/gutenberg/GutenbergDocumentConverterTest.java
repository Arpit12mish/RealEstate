package com.brandPitara.sfs.migration.wordpress.gutenberg;

import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.HeadingLevel;
import com.brandPitara.sfs.cms.content.document.InlineNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GutenbergDocumentConverterTest {

    private final GutenbergDocumentConverter converter = new GutenbergDocumentConverter();

    private GutenbergConversionResult convert(String postContent) {
        return converter.convert(1L, "Test Post", postContent);
    }

    @Test
    void paragraphConvertsPreservingTextAndMarks() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p>Read our <strong>Gurgaon</strong> <em>guide</em> <a href="https://squarefootstory.com/g">here</a>.</p>
                <!-- /wp:paragraph -->
                """);
        assertThat(result.document().blocks()).hasSize(1);
        ContentBlock.Paragraph paragraph = (ContentBlock.Paragraph) result.document().blocks().get(0);
        assertThat(paragraph.content()).hasSizeGreaterThan(1);
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
        assertThat(result.document().schemaVersion()).isEqualTo(5);
    }

    @Test
    void headingLevelsMapDirectlyAndOutOfRangeLevelsDowngradeWithWarning() {
        assertThat(headingLevel(2)).isEqualTo(HeadingLevel.H2);
        assertThat(headingLevel(3)).isEqualTo(HeadingLevel.H3);
        assertThat(headingLevel(4)).isEqualTo(HeadingLevel.H4);

        GutenbergConversionResult h1 = convert(heading(1));
        assertThat(((ContentBlock.Heading) h1.document().blocks().get(0)).level()).isEqualTo(HeadingLevel.H2);
        assertThat(h1.warnings()).isNotEmpty();

        GutenbergConversionResult h6 = convert(heading(6));
        assertThat(((ContentBlock.Heading) h6.document().blocks().get(0)).level()).isEqualTo(HeadingLevel.H4);
        assertThat(h6.warnings()).isNotEmpty();
    }

    private HeadingLevel headingLevel(int wpLevel) {
        return ((ContentBlock.Heading) convert(heading(wpLevel)).document().blocks().get(0)).level();
    }

    private String heading(int level) {
        return "<!-- wp:heading {\"level\":" + level + "} -->\n<h" + level + ">Title</h" + level + ">\n<!-- /wp:heading -->";
    }

    @Test
    void unorderedAndOrderedListsWithListItemChildrenConvertPreservingOrder() {
        GutenbergConversionResult result = convert("""
                <!-- wp:list {"ordered":false} -->
                <ul><!-- wp:list-item -->
                <li>First</li>
                <!-- /wp:list-item -->

                <!-- wp:list-item -->
                <li>Second</li>
                <!-- /wp:list-item --></ul>
                <!-- /wp:list -->
                """);
        ContentBlock.BulletList list = (ContentBlock.BulletList) result.document().blocks().get(0);
        assertThat(list.items()).hasSize(2);
        assertThat(text(list.items().get(0).content())).isEqualTo("First");
        assertThat(text(list.items().get(1).content())).isEqualTo("Second");
    }

    @Test
    void oldStyleListWithoutListItemChildrenStillConverts() {
        GutenbergConversionResult result = convert("""
                <!-- wp:list {"ordered":true} -->
                <ol><li>One</li><li>Two</li></ol>
                <!-- /wp:list -->
                """);
        ContentBlock.OrderedList list = (ContentBlock.OrderedList) result.document().blocks().get(0);
        assertThat(list.items()).hasSize(2);
    }

    @Test
    void quoteConvertsToBlockquoteAndCitationBecomesAWarningNotContent() {
        GutenbergConversionResult result = convert("""
                <!-- wp:quote -->
                <blockquote class="wp-block-quote"><p>Good design matters.</p><cite>Dieter Rams</cite></blockquote>
                <!-- /wp:quote -->
                """);
        ContentBlock.Blockquote quote = (ContentBlock.Blockquote) result.document().blocks().get(0);
        assertThat(text(quote.content())).isEqualTo("Good design matters.");
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("Dieter Rams"));
    }

    @Test
    void separatorConvertsToDivider() {
        GutenbergConversionResult result = convert("<!-- wp:separator /-->");
        assertThat(result.document().blocks().get(0)).isInstanceOf(ContentBlock.Divider.class);
    }

    @Test
    void imageWithAttachmentIdConvertsAndIsTrackedAsReferenced() {
        GutenbergConversionResult result = convert("""
                <!-- wp:image {"id":481,"sizeSlug":"large"} -->
                <figure class="wp-block-image size-large"><img src="https://squarefootstory.com/x.jpg" alt="Skyline" class="wp-image-481"/><figcaption>Gurgaon skyline</figcaption></figure>
                <!-- /wp:image -->
                """);
        ContentBlock.Image image = (ContentBlock.Image) result.document().blocks().get(0);
        assertThat(image.mediaAssetId()).isEqualTo(481L);
        assertThat(image.altText()).isEqualTo("Skyline");
        assertThat(text(image.caption())).isEqualTo("Gurgaon skyline");
        assertThat(result.referencedAttachmentIds()).containsExactly(481L);
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void imageWithoutResolvableAttachmentIdIsUnresolvedAndForcesManualReview() {
        GutenbergConversionResult result = convert("""
                <!-- wp:image -->
                <figure class="wp-block-image"><img src="https://external.example/hotlinked.jpg" alt="External"/></figure>
                <!-- /wp:image -->
                """);
        assertThat(result.document().blocks()).isEmpty();
        assertThat(result.referencedExternalUrls()).containsExactly("https://external.example/hotlinked.jpg");
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.eligibleForAutomaticPublication()).isFalse();
        assertThat(result.unsupportedBlocks()).anyMatch(b -> b.blockName().equals("core/image") && b.hadMeaningfulContent());
    }

    @Test
    void galleryWithNestedImageChildrenConvertsToImageGallery() {
        GutenbergConversionResult result = convert("""
                <!-- wp:gallery {"columns":3} -->
                <figure class="wp-block-gallery"><!-- wp:image {"id":601} -->
                <figure><img src="a.jpg" alt="Tower A" class="wp-image-601"/></figure>
                <!-- /wp:image -->

                <!-- wp:image {"id":602} -->
                <figure><img src="b.jpg" alt="Tower B" class="wp-image-602"/></figure>
                <!-- /wp:image --></figure>
                <!-- /wp:gallery -->
                """);
        ContentBlock.Gallery gallery = (ContentBlock.Gallery) result.document().blocks().get(0);
        assertThat(gallery.columns()).isEqualTo(3);
        assertThat(gallery.images()).hasSize(2);
        assertThat(result.referencedAttachmentIds()).containsExactlyInAnyOrder(601L, 602L);
    }

    @Test
    void galleryWithAllImagesUnresolvedPreservesAStructuredUnresolvedGalleryRecord() {
        // Mirrors real post 8784: a genuine core/gallery block whose images are hotlinked from
        // an external host with no WordPress attachment ID at all.
        GutenbergConversionResult result = convert("""
                <!-- wp:gallery {"linkTo":"none"} -->
                <figure class="wp-block-gallery has-nested-images columns-default"><!-- wp:image -->
                <figure class="wp-block-image"><img src="https://external.example/a.jpg" alt=""/></figure>
                <!-- /wp:image -->

                <!-- wp:image -->
                <figure class="wp-block-image"><img src="https://external.example/b.jpg" alt=""/></figure>
                <!-- /wp:image --></figure>
                <!-- /wp:gallery -->
                """);

        // The gallery is not silently omitted from the document either way - it is a
        // MANUAL_REVIEW_REQUIRED unsupported block, never a fabricated ContentBlock.Gallery.
        assertThat(result.document().blocks()).isEmpty();
        assertThat(result.unsupportedBlocks()).anyMatch(b -> b.blockName().equals("core/gallery"));
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.eligibleForAutomaticPublication()).isFalse();

        assertThat(result.unresolvedGalleries()).hasSize(1);
        UnresolvedGalleryReport report = result.unresolvedGalleries().get(0);
        assertThat(report.fullyUnresolved()).isTrue();
        assertThat(report.resolvedAttachmentIds()).isEmpty();
        assertThat(report.unresolvedImageUrls()).containsExactly(
                "https://external.example/a.jpg", "https://external.example/b.jpg"
        );
        assertThat(result.referencedExternalUrls()).containsExactlyInAnyOrder(
                "https://external.example/a.jpg", "https://external.example/b.jpg"
        );
        // Never placed into the document as a permanent WordPress URL or a fabricated media ID.
        assertThat(result.document().blocks()).noneMatch(b -> b.toString().contains("external.example"));
    }

    @Test
    void galleryWithSomeImagesResolvedStillRecordsTheUnresolvedOnesForRepair() {
        GutenbergConversionResult result = convert("""
                <!-- wp:gallery -->
                <figure class="wp-block-gallery"><!-- wp:image {"id":601} -->
                <figure><img src="a.jpg" alt="Tower A" class="wp-image-601"/></figure>
                <!-- /wp:image -->

                <!-- wp:image -->
                <figure><img src="https://external.example/c.jpg" alt=""/></figure>
                <!-- /wp:image --></figure>
                <!-- /wp:gallery -->
                """);

        ContentBlock.Gallery gallery = (ContentBlock.Gallery) result.document().blocks().get(0);
        assertThat(gallery.images()).hasSize(1);
        assertThat(result.unresolvedGalleries()).hasSize(1);
        UnresolvedGalleryReport report = result.unresolvedGalleries().get(0);
        assertThat(report.fullyUnresolved()).isFalse();
        assertThat(report.resolvedAttachmentIds()).containsExactly(601L);
        assertThat(report.unresolvedImageUrls()).containsExactly("https://external.example/c.jpg");
    }

    @Test
    void galleryWithoutImageChildrenFallsBackToParsingImgTagsDirectly() {
        GutenbergConversionResult result = convert("""
                <!-- wp:gallery -->
                <figure class="wp-block-gallery"><ul><li><figure><img src="a.jpg" alt="A" class="wp-image-701"/></figure></li>
                <li><figure><img src="b.jpg" alt="B" class="wp-image-702"/></figure></li></ul></figure>
                <!-- /wp:gallery -->
                """);
        ContentBlock.Gallery gallery = (ContentBlock.Gallery) result.document().blocks().get(0);
        assertThat(gallery.images()).hasSize(2);
    }

    @Test
    void videoWithAttachmentIdConverts() {
        GutenbergConversionResult result = convert("""
                <!-- wp:video {"id":512} -->
                <figure class="wp-block-video"><video controls src="interview.mp4" class="wp-video-512"></video><figcaption>Founder interview</figcaption></figure>
                <!-- /wp:video -->
                """);
        ContentBlock.Video video = (ContentBlock.Video) result.document().blocks().get(0);
        assertThat(video.mediaAssetId()).isEqualTo(512L);
        assertThat(result.referencedAttachmentIds()).containsExactly(512L);
    }

    @Test
    void youTubeEmbedConvertsToEmbedBlock() {
        GutenbergConversionResult result = convert("""
                <!-- wp:embed {"url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ","providerNameSlug":"youtube"} -->
                <figure class="wp-block-embed"><div class="wp-block-embed__wrapper">
                https://www.youtube.com/watch?v=dQw4w9WgXcQ
                </div></figure>
                <!-- /wp:embed -->
                """);
        ContentBlock.Embed embed = (ContentBlock.Embed) result.document().blocks().get(0);
        assertThat(embed.externalId()).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void nonYouTubeEmbedIsUnsupportedAndRequiresManualReview() {
        GutenbergConversionResult result = convert("""
                <!-- wp:embed {"url":"https://vimeo.com/12345","providerNameSlug":"vimeo"} -->
                <figure class="wp-block-embed"></figure>
                <!-- /wp:embed -->
                """);
        assertThat(result.document().blocks()).isEmpty();
        assertThat(result.manualReviewRequired()).isTrue();
    }

    @Test
    void tableConvertsHeaderAndBodyRows() {
        GutenbergConversionResult result = convert("""
                <!-- wp:table -->
                <figure class="wp-block-table"><table><thead><tr><th>Package</th><th>Price</th></tr></thead>
                <tbody><tr><td>Essential</td><td>1200</td></tr></tbody></table></figure>
                <!-- /wp:table -->
                """);
        ContentBlock.Table table = (ContentBlock.Table) result.document().blocks().get(0);
        assertThat(table.columns()).extracting(ContentBlock.TableColumn::label).containsExactly("Package", "Price");
        assertThat(table.rows()).hasSize(1);
    }

    @Test
    void columnsOfOnlyImagesConvertsToValidatedLayout() {
        GutenbergConversionResult result = convert("""
                <!-- wp:columns -->
                <div class="wp-block-columns"><!-- wp:column -->
                <div class="wp-block-column"><!-- wp:image {"id":1} -->
                <figure><img src="a.jpg" alt="A" class="wp-image-1"/></figure>
                <!-- /wp:image --></div>
                <!-- /wp:column -->

                <!-- wp:column -->
                <div class="wp-block-column"><!-- wp:image {"id":2} -->
                <figure><img src="b.jpg" alt="B" class="wp-image-2"/></figure>
                <!-- /wp:image --></div>
                <!-- /wp:column --></div>
                <!-- /wp:columns -->
                """);
        ContentBlock.Layout layout = (ContentBlock.Layout) result.document().blocks().get(0);
        assertThat(layout.children()).hasSize(2);
    }

    @Test
    void columnsOfMixedContentFlattensInReadingOrderInsteadOfForcingLayout() {
        GutenbergConversionResult result = convert("""
                <!-- wp:columns -->
                <div class="wp-block-columns"><!-- wp:column -->
                <div class="wp-block-column"><!-- wp:paragraph -->
                <p>Left text</p>
                <!-- /wp:paragraph --></div>
                <!-- /wp:column -->

                <!-- wp:column -->
                <div class="wp-block-column"><!-- wp:paragraph -->
                <p>Right text</p>
                <!-- /wp:paragraph --></div>
                <!-- /wp:column --></div>
                <!-- /wp:columns -->
                """);
        assertThat(result.document().blocks()).hasSize(2).allMatch(ContentBlock.Paragraph.class::isInstance);
        assertThat(text(((ContentBlock.Paragraph) result.document().blocks().get(0)).content())).isEqualTo("Left text");
        assertThat(text(((ContentBlock.Paragraph) result.document().blocks().get(1)).content())).isEqualTo("Right text");
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("Flattened"));
    }

    @Test
    void mediaTextFlattensToImageThenText() {
        GutenbergConversionResult result = convert("""
                <!-- wp:media-text {"mediaId":481,"mediaAlt":"Clubhouse"} -->
                <div class="wp-block-media-text"><figure class="wp-block-media-text__media"><img src="c.jpg" alt="Clubhouse" class="wp-image-481"/></figure>
                <div class="wp-block-media-text__content"><!-- wp:paragraph -->
                <p>Amenities include a clubhouse.</p>
                <!-- /wp:paragraph --></div></div>
                <!-- /wp:media-text -->
                """);
        assertThat(result.document().blocks()).hasSize(2);
        assertThat(result.document().blocks().get(0)).isInstanceOf(ContentBlock.Image.class);
        assertThat(result.document().blocks().get(1)).isInstanceOf(ContentBlock.Paragraph.class);
    }

    @Test
    void spacerIsOmittedWithInformationalWarningOnly() {
        GutenbergConversionResult result = convert("<!-- wp:spacer {\"height\":\"40px\"} /-->");
        assertThat(result.document().blocks()).isEmpty();
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.manualReviewRequired()).isFalse();
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void ratingWidgetIsOmittedWithWarningAndDoesNotBlockPublication() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p>Real content.</p>
                <!-- /wp:paragraph -->
                <!-- wp:feedbackwp/rating-widget /-->
                """);
        assertThat(result.document().blocks()).hasSize(1);
        assertThat(result.warnings()).anyMatch(w -> w.blockName().equals("feedbackwp/rating-widget"));
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void qliggBoxIsUnsupportedManualReviewBlocker() {
        GutenbergConversionResult result = convert("""
                <!-- wp:qligg/box -->
                <div>Some quiz content</div>
                <!-- /wp:qligg/box -->
                """);
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.eligibleForAutomaticPublication()).isFalse();
        assertThat(result.unsupportedBlocks()).anyMatch(b ->
                b.blockName().equals("qligg/box") && b.disposition() == UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
        );
    }

    @Test
    void safeCoreHtmlConvertsBestEffortAsParagraphWithWarning() {
        GutenbergConversionResult result = convert("""
                <!-- wp:html -->
                <div><p>Plain <strong>safe</strong> markup.</p></div>
                <!-- /wp:html -->
                """);
        assertThat(result.document().blocks()).hasSize(1);
        assertThat(result.document().blocks().get(0)).isInstanceOf(ContentBlock.Paragraph.class);
        assertThat(result.warnings()).isNotEmpty();
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void unsafeCoreHtmlWithScriptRequiresManualReviewAndIsNeverInjected() {
        GutenbergConversionResult result = convert("""
                <!-- wp:html -->
                <script>alert('x')</script><p>text</p>
                <!-- /wp:html -->
                """);
        assertThat(result.document().blocks()).isEmpty();
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.eligibleForAutomaticPublication()).isFalse();
    }

    @Test
    void emptyCoreHtmlIsOmittedSilentlyWithoutBlockingPublication() {
        GutenbergConversionResult result = convert("""
                <!-- wp:html -->
                <!-- an empty tracking div --><div></div>
                <!-- /wp:html -->
                """);
        assertThat(result.document().blocks()).isEmpty();
        assertThat(result.manualReviewRequired()).isFalse();
    }

    @Test
    void unknownBlockWithMeaningfulContentIsReportedAndRequiresManualReview() {
        GutenbergConversionResult result = convert("""
                <!-- wp:some-plugin/widget -->
                <div>Interesting widget text</div>
                <!-- /wp:some-plugin/widget -->
                """);
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.unsupportedBlocks()).anyMatch(b ->
                b.blockName().equals("some-plugin/widget") && b.hadMeaningfulContent()
                        && b.disposition() == UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
        );
    }

    @Test
    void unknownBlockWithNoMeaningfulContentIsOmittedWithoutBlockingPublication() {
        GutenbergConversionResult result = convert("""
                <!-- wp:some-plugin/empty-wrapper -->
                <div></div>
                <!-- /wp:some-plugin/empty-wrapper -->
                """);
        assertThat(result.manualReviewRequired()).isFalse();
        assertThat(result.unsupportedBlocks()).anyMatch(b ->
                b.disposition() == UnsupportedBlockReport.Disposition.OMITTED_NO_CONTENT
        );
    }

    @Test
    void freeformClassicContentConvertsBestEffortWithWarning() {
        GutenbergConversionResult result = convert("<p>Classic editor paragraph with no block markers.</p>");
        assertThat(result.document().blocks()).hasSize(1);
        assertThat(result.warnings()).anyMatch(w -> w.blockName().equals("(freeform)"));
    }

    @Test
    void blankFreeformWhitespaceBetweenBlocksIsIgnoredSilently() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p>A</p>
                <!-- /wp:paragraph -->

                <!-- wp:paragraph -->
                <p>B</p>
                <!-- /wp:paragraph -->
                """);
        assertThat(result.document().blocks()).hasSize(2);
        assertThat(result.warnings()).noneMatch(w -> "(freeform)".equals(w.blockName()));
    }

    // ── Link remediation (Section 2) ────────────────────────────────────────────

    @Test
    void hashOnlyLinkIsStrippedWithWarningTextPreservedNotManualReview() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p><a href="#">Click here</a> to learn more.</p>
                <!-- /wp:paragraph -->
                """);
        ContentBlock.Paragraph paragraph = (ContentBlock.Paragraph) result.document().blocks().get(0);
        assertThat(text(paragraph.content())).contains("Click here");
        assertThat(paragraph.content()).noneMatch(node -> node instanceof InlineNode.Text t
                && t.marks().stream().anyMatch(m -> m instanceof com.brandPitara.sfs.cms.content.document.TextMark.Link));
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("no real destination"));
        assertThat(result.manualReviewRequired()).isFalse();
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void protocolRelativeLinkIsUpgradedToHttps() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p><a href="//squarefootstory.com/guide">Guide</a></p>
                <!-- /wp:paragraph -->
                """);
        ContentBlock.Paragraph paragraph = (ContentBlock.Paragraph) result.document().blocks().get(0);
        String href = linkHref(paragraph.content());
        assertThat(href).isEqualTo("https://squarefootstory.com/guide");
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("Protocol-relative"));
    }

    @Test
    void plainHttpLinkIsUpgradedToHttps() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p><a href="http://housing.com">Housing</a></p>
                <!-- /wp:paragraph -->
                """);
        ContentBlock.Paragraph paragraph = (ContentBlock.Paragraph) result.document().blocks().get(0);
        assertThat(linkHref(paragraph.content())).isEqualTo("https://housing.com");
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("upgraded to https"));
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void bareRelativeReferenceIsNormalizedToRootRelativePath() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p><a href="about-us">About</a></p>
                <!-- /wp:paragraph -->
                """);
        ContentBlock.Paragraph paragraph = (ContentBlock.Paragraph) result.document().blocks().get(0);
        assertThat(linkHref(paragraph.content())).isEqualTo("/about-us");
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("root-relative"));
    }

    @Test
    void unsafeLinkSchemeIsStrippedTextPreservedAndFlaggedForManualReview() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p><a href="javascript:alert(1)">Danger</a> zone.</p>
                <!-- /wp:paragraph -->
                """);
        ContentBlock.Paragraph paragraph = (ContentBlock.Paragraph) result.document().blocks().get(0);
        assertThat(text(paragraph.content())).contains("Danger");
        assertThat(paragraph.content()).noneMatch(node -> node instanceof InlineNode.Text t
                && t.marks().stream().anyMatch(m -> m instanceof com.brandPitara.sfs.cms.content.document.TextMark.Link));
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(result.eligibleForAutomaticPublication()).isFalse();
        // Never emits the dangerous scheme anywhere in the final document.
        assertThat(result.document().toString()).doesNotContain("javascript:");
    }

    @Test
    void mailtoLinkIsStrippedAndFlaggedForManualReviewNotSilentlyKept() {
        GutenbergConversionResult result = convert("""
                <!-- wp:paragraph -->
                <p>Email <a href="mailto:editor@example.com">us</a>.</p>
                <!-- /wp:paragraph -->
                """);
        assertThat(result.manualReviewRequired()).isTrue();
        assertThat(text(((ContentBlock.Paragraph) result.document().blocks().get(0)).content())).contains("us");
    }

    private String linkHref(List<InlineNode> nodes) {
        for (InlineNode node : nodes) {
            if (node instanceof InlineNode.Text text) {
                for (var mark : text.marks()) {
                    if (mark instanceof com.brandPitara.sfs.cms.content.document.TextMark.Link link) {
                        return link.href();
                    }
                }
            }
        }
        return null;
    }

    // ── YouTube remediation (Section 2) ─────────────────────────────────────────

    @Test
    void youTubeLiveUrlExtractsTheIdUnambiguously() {
        GutenbergConversionResult result = convert("""
                <!-- wp:embed {"url":"https://www.youtube.com/live/xYSdwSBJJE4?si=abc123","providerNameSlug":"youtube"} -->
                <figure class="wp-block-embed"></figure>
                <!-- /wp:embed -->
                """);
        ContentBlock.Embed embed = (ContentBlock.Embed) result.document().blocks().get(0);
        assertThat(embed.externalId()).isEqualTo("xYSdwSBJJE4");
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void unrecognizedYouTubeUrlFormatIsPreservedAsPlainTextAndFlaggedForManualReview() {
        GutenbergConversionResult result = convert("""
                <!-- wp:embed {"url":"https://www.youtube.com/playlist?list=PLsomething","providerNameSlug":"youtube"} -->
                <figure class="wp-block-embed"></figure>
                <!-- /wp:embed -->
                """);
        assertThat(result.document().blocks()).hasSize(1);
        assertThat(result.document().blocks().get(0)).isInstanceOf(ContentBlock.Paragraph.class);
        assertThat(text(((ContentBlock.Paragraph) result.document().blocks().get(0)).content()))
                .contains("youtube.com/playlist");
        assertThat(result.manualReviewRequired()).isTrue();
        // Never an invalid/malformed EMBED block.
        assertThat(result.document().blocks()).noneMatch(ContentBlock.Embed.class::isInstance);
    }

    // ── Alt-text remediation (Section 2) ────────────────────────────────────────

    @Test
    void altTextWithEmbeddedControlCharactersIsSanitizedNotBlocked() {
        GutenbergConversionResult result = convert("""
                <!-- wp:image {"id":481} -->
                <figure><img src="a.jpg" alt="Myra Homes&#10;Chattarpur, South Delhi" class="wp-image-481"/></figure>
                <!-- /wp:image -->
                """);
        ContentBlock.Image image = (ContentBlock.Image) result.document().blocks().get(0);
        assertThat(image.altText()).isEqualTo("Myra Homes Chattarpur, South Delhi");
        assertThat(image.altText().codePoints()).noneMatch(Character::isISOControl);
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("control characters removed"));
        assertThat(result.eligibleForAutomaticPublication()).isTrue();
    }

    @Test
    void oversizedAltTextIsTruncatedAtAWordBoundaryNotMidWord() {
        String longAlt = ("Gurgaon skyline view of the tallest residential towers near the expressway ").repeat(6);
        assertThat(longAlt.length()).isGreaterThan(300);
        GutenbergConversionResult result = convert("""
                <!-- wp:image {"id":481} -->
                <figure><img src="a.jpg" alt="%s" class="wp-image-481"/></figure>
                <!-- /wp:image -->
                """.formatted(longAlt));
        ContentBlock.Image image = (ContentBlock.Image) result.document().blocks().get(0);
        assertThat(image.altText().length()).isLessThanOrEqualTo(300);
        assertThat(image.altText()).doesNotEndWith(" ");
        assertThat(longAlt).startsWith(image.altText());
        assertThat(result.warnings()).anyMatch(w -> w.message().contains("truncated at a word boundary"));
    }

    @Test
    void deterministicRerunProducesAnIdenticalDocument() {
        String content = """
                <!-- wp:heading {"level":2} --><h2>Overview</h2><!-- /wp:heading -->
                <!-- wp:paragraph --><p>Stable text.</p><!-- /wp:paragraph -->
                """;
        GutenbergConversionResult first = convert(content);
        GutenbergConversionResult second = convert(content);
        assertThat(first.document()).isEqualTo(second.document());
        assertThat(first.referencedAttachmentIds()).isEqualTo(second.referencedAttachmentIds());
    }

    private String text(java.util.List<InlineNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (InlineNode node : nodes) {
            if (node instanceof InlineNode.Text text) {
                sb.append(text.text());
            }
        }
        return sb.toString();
    }
}
