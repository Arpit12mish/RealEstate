package com.brandPitara.sfs.cms.content.document;

import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentDocumentValidatorTest {

    private ObjectMapper objectMapper;
    private ContentDocumentValidator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        validator = new ContentDocumentValidator(objectMapper);
    }

    @Test
    void acceptsEmptyDocumentAndAllSupportedBlockAndInlineTypes() throws Exception {
        ContentDocument document = new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                new ContentBlock.Paragraph(List.of(
                        new InlineNode.Text("Read our ", List.of(new TextMark.Bold())),
                        new InlineNode.Text("Gurgaon guide", List.of(
                                new TextMark.Italic(),
                                new TextMark.Underline(),
                                new TextMark.Link("https://squarefootstory.com/guide", true, true, false)
                        )),
                        new InlineNode.HardBreak()
                )),
                new ContentBlock.Heading(HeadingLevel.H2, text("Market overview")),
                new ContentBlock.Heading(HeadingLevel.H3, text("Demand")),
                new ContentBlock.Heading(HeadingLevel.H4, text("Outlook")),
                new ContentBlock.Blockquote(text("Location remains decisive.")),
                new ContentBlock.BulletList(List.of(
                        new ContentBlock.ListItem(text("Golf Course Road")),
                        new ContentBlock.ListItem(text("Dwarka Expressway"))
                )),
                new ContentBlock.OrderedList(List.of(
                        new ContentBlock.ListItem(text("Set a budget")),
                        new ContentBlock.ListItem(text("Compare projects"))
                )),
                new ContentBlock.Divider()
        ));

        ContentDocument normalized = validator.validateAndNormalize(document);
        String json = objectMapper.writeValueAsString(normalized);
        ContentDocument roundTrip = objectMapper.readValue(json, ContentDocument.class);

        assertThat(validator.validateAndNormalize(ContentDocument.empty()).blocks()).isEmpty();
        assertThat(roundTrip).isEqualTo(normalized);
        assertThat(json).contains("\"type\":\"PARAGRAPH\"")
                .contains("\"type\":\"HARD_BREAK\"")
                .contains("\"type\":\"LINK\"");
    }

    @Test
    void normalizesOnlyLineEndingsAndLinksWithoutTrimmingEditorialText() {
        ContentDocument document = new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                new ContentBlock.Paragraph(List.of(
                        new InlineNode.Text("  first\r\nsecond  ", List.of(
                                new TextMark.Link(" https://EXAMPLE.com/a/../guide ", false, false, false)
                        ))
                ))
        ));

        ContentDocument normalized = validator.validateAndNormalize(document);
        InlineNode.Text text = (InlineNode.Text) ((ContentBlock.Paragraph) normalized.blocks().get(0))
                .content().get(0);
        TextMark.Link link = (TextMark.Link) text.marks().get(0);

        assertThat(text.text()).isEqualTo("  first\nsecond  ");
        assertThat(link.href()).isEqualTo("https://EXAMPLE.com/guide");
    }

    @Test
    void markupLookingCharactersRemainPlainTextForRendererEscaping() {
        String editorialText = "<script>alert('text only')</script><img onerror=alert(1)>";
        ContentDocument normalized = validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                new ContentBlock.Paragraph(List.of(
                        new InlineNode.Text(editorialText, List.of())
                ))
        )));

        InlineNode.Text text = (InlineNode.Text) ((ContentBlock.Paragraph)
                normalized.blocks().get(0)).content().get(0);
        assertThat(text.text()).isEqualTo(editorialText);
    }

    @Test
    void permitsHttpsAndRootRelativeLinksButRejectsUnsafeSchemesAndProtocolRelativeLinks() {
        for (String safe : List.of(
                "https://squarefootstory.com/gurgaon?type=blog#market",
                "/blog/gurgaon?source=cms#market"
        )) {
            assertThat(validator.validateAndNormalize(documentWithLink(safe))).isNotNull();
        }

        for (String unsafe : List.of(
                "javascript:alert(1)",
                "data:text/html,boom",
                "file:///tmp/secret",
                "vbscript:msgbox(1)",
                "http://example.com",
                "mailto:editor@example.com",
                "tel:+911234567890",
                "//evil.example/path",
                "relative/path"
        )) {
            assertCode(() -> validator.validateAndNormalize(documentWithLink(unsafe)),
                    "CONTENT_DOCUMENT_INVALID");
        }
    }

    @Test
    void rejectsUnsupportedSchemaNullsEmptySemanticBlocksDuplicateMarksAndOversizedStructures() {
        assertCode(() -> validator.validateAndNormalize(
                        new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION + 1, List.of())),
                "CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED");
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, null)),
                "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                        new ContentBlock.Heading(HeadingLevel.H2, List.of())
                ))), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                        new ContentBlock.BulletList(List.of())
                ))), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                        new ContentBlock.Paragraph(List.of(
                                new InlineNode.Text("duplicate", List.of(
                                        new TextMark.Bold(), new TextMark.Bold()
                                ))
                        ))
                ))), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION,
                        java.util.Collections.nCopies(
                                ContentDocumentLimits.MAX_BLOCKS + 1,
                                new ContentBlock.Divider()
                        ))), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                        new ContentBlock.Paragraph(List.of(new InlineNode.Text(
                                "x".repeat(ContentDocumentLimits.MAX_TEXT_NODE_CHARACTERS + 1),
                                List.of()
                        )))
                ))), "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void rejectsDocumentWhoseUtf8SerializationExceedsBoundedPayload() {
        List<ContentBlock> blocks = new ArrayList<>();
        for (int index = 0; index < 14; index++) {
            blocks.add(new ContentBlock.Paragraph(List.of(
                    new InlineNode.Text("界".repeat(20_000), List.of())
            )));
        }

        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, blocks)),
                "CONTENT_DOCUMENT_TOO_LARGE");
    }

    @Test
    void acceptsEachStructuralLimitAtBoundaryAndRejectsOneAbove() {
        assertThat(validator.validateAndNormalize(new ContentDocument(2,
                java.util.Collections.nCopies(ContentDocumentLimits.MAX_BLOCKS, new ContentBlock.Divider()))))
                .isNotNull();
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(2,
                java.util.Collections.nCopies(ContentDocumentLimits.MAX_BLOCKS + 1, new ContentBlock.Divider()))),
                "CONTENT_DOCUMENT_INVALID");

        assertThat(validator.validateAndNormalize(new ContentDocument(2, List.of(
                new ContentBlock.Paragraph(List.of(new InlineNode.Text(
                        "x".repeat(ContentDocumentLimits.MAX_TEXT_NODE_CHARACTERS), List.of())))
        )))).isNotNull();
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(2, List.of(
                new ContentBlock.Paragraph(List.of(new InlineNode.Text(
                        "x".repeat(ContentDocumentLimits.MAX_TEXT_NODE_CHARACTERS + 1), List.of())))
        ))), "CONTENT_DOCUMENT_INVALID");

        var item = new ContentBlock.ListItem(text("item"));
        assertThat(validator.validateAndNormalize(new ContentDocument(2, List.of(
                new ContentBlock.BulletList(java.util.Collections.nCopies(ContentDocumentLimits.MAX_LIST_ITEMS, item))
        )))).isNotNull();
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(2, List.of(
                new ContentBlock.BulletList(java.util.Collections.nCopies(ContentDocumentLimits.MAX_LIST_ITEMS + 1, item))
        ))), "CONTENT_DOCUMENT_INVALID");

        assertThat(validator.validateAndNormalize(mediaDocument(new ContentBlock.Image(
                1L, false, "image", text("x".repeat(ContentDocumentLimits.MAX_CAPTION_CHARACTERS)), null, null
        )))).isNotNull();
        assertCode(() -> validator.validateAndNormalize(mediaDocument(new ContentBlock.Image(
                1L, false, "image", text("x".repeat(ContentDocumentLimits.MAX_CAPTION_CHARACTERS + 1)), null, null
        ))), "CONTENT_DOCUMENT_INVALID");

        java.util.List<ContentBlock> exactlyMaximumText = new java.util.ArrayList<>();
        for (int index = 0; index < 25; index++) {
            exactlyMaximumText.add(new ContentBlock.Paragraph(text(
                    "x".repeat(ContentDocumentLimits.MAX_TEXT_NODE_CHARACTERS))));
        }
        assertThat(validator.validateAndNormalize(new ContentDocument(2, exactlyMaximumText))).isNotNull();
        exactlyMaximumText.add(new ContentBlock.Paragraph(text("x")));
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(2, exactlyMaximumText)),
                "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void jacksonRejectsUnknownPropertiesTypesRawHtmlScriptsMediaAndH1() {
        for (String invalidDocument : List.of(
                """
                {"schemaVersion":1,"blocks":[],"frontendState":{}}
                """,
                documentJson("{\"type\":\"RAW_HTML\",\"html\":\"<script>x</script>\"}"),
                documentJson("{\"type\":\"SCRIPT\",\"code\":\"alert(1)\"}"),
                documentJson("{\"type\":\"IMAGE\",\"src\":\"https://example.com/a.jpg\"}"),
                documentJson("{\"type\":\"VIDEO\",\"src\":\"https://example.com/a.mp4\"}"),
                documentJson("{\"type\":\"PARAGRAPH\",\"content\":[{\"type\":\"TEXT\",\"text\":\"x\",\"marks\":[{\"type\":\"COLOR\"}]}]}"),
                documentJson("{\"type\":\"HEADING\",\"level\":\"H1\",\"content\":[{\"type\":\"TEXT\",\"text\":\"x\",\"marks\":[]}]}"),
                documentJson("{\"type\":\"DIVIDER\",\"style\":\"danger\"}")
        )) {
            assertThatThrownBy(() -> objectMapper.readValue(invalidDocument, ContentDocument.class))
                    .isInstanceOf(Exception.class);
        }
    }

    @Test
    void wordCountIsDeterministicAcrossBlocksAndIgnoresHardBreaksAndDivider() {
        ContentDocument document = new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                new ContentBlock.Paragraph(List.of(
                        new InlineNode.Text("Gurgaon's well-connected market", List.of()),
                        new InlineNode.HardBreak(),
                        new InlineNode.Text("grew 20 percent", List.of())
                )),
                new ContentBlock.Heading(HeadingLevel.H2, text("Next steps")),
                new ContentBlock.BulletList(List.of(
                        new ContentBlock.ListItem(text("Compare homes"))
                )),
                new ContentBlock.Divider()
        ));

        assertThat(new ContentDocumentWordCounter().count(document)).isEqualTo(10);
    }

    @Test
    void wordCountCountsNewBlockTextButNotEnumLabels() {
        ContentDocument document = validator.validateAndNormalize(v3Document(
                new ContentBlock.CheckList(List.of(
                        new ContentBlock.ListItem(text("Smart switches")) // 2 words
                )),
                new ContentBlock.Callout(CalloutVariant.VERDICT, "SFS Verdict", text("Strong pick")), // 3 + 2 words
                new ContentBlock.Table(
                        "Project Snapshot", // 2 words
                        List.of(new ContentBlock.TableColumn("Package")), // 1 word
                        List.of(new ContentBlock.TableRow(TableRowType.TOTAL, List.of(text("Seven towers")))) // 2 words
                )
        ));

        // 2 ("Smart switches") + (2 "SFS Verdict" title + 2 "Strong pick" content)
        // + (2 "Project Snapshot" caption + 1 "Package" column label + 2 "Seven towers" cell) = 11.
        // "VERDICT" and "TOTAL" are enum labels, never fed through the word counter's
        // InlineNode/String scanning, so they cannot inflate this count.
        assertThat(new ContentDocumentWordCounter().count(document)).isEqualTo(11);
    }

    @Test
    void acceptsAndNormalizesVersionTwoMediaBlocksAndCountsOnlyCaptions() throws Exception {
        ContentDocument document = new ContentDocument(2, List.of(
                new ContentBlock.Image(
                        10L, false, "  Apartment clubhouse  ", text("Central lawn view"), null,
                        new ContentLink(" /projects/27 ", false, false, false)
                ),
                new ContentBlock.Image(11L, true, "  ", null, ImageLayout.WIDE, null),
                new ContentBlock.Video(20L, 12L, text("Founder interview")),
                new ContentBlock.Embed(
                        EmbedProvider.YOUTUBE,
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share",
                        text("Market walkthrough")
                )
        ));

        ContentDocument normalized = validator.validateAndNormalize(document);
        ContentBlock.Image image = (ContentBlock.Image) normalized.blocks().get(0);
        ContentBlock.Image decorative = (ContentBlock.Image) normalized.blocks().get(1);
        ContentBlock.Embed embed = (ContentBlock.Embed) normalized.blocks().get(3);

        assertThat(normalized.schemaVersion()).isEqualTo(ContentDocument.CURRENT_SCHEMA_VERSION);
        assertThat(image.altText()).isEqualTo("Apartment clubhouse");
        assertThat(image.layout()).isEqualTo(ImageLayout.STANDARD);
        assertThat(image.link().href()).isEqualTo("/projects/27");
        assertThat(decorative.altText()).isNull();
        assertThat(embed.externalId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(new ContentDocumentWordCounter().count(normalized)).isEqualTo(7);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                .isEqualTo(normalized);
    }

    @Test
    void enforcesImageAccessibilityCaptionAndSafeLinkRules() {
        assertCode(() -> validator.validateAndNormalize(mediaDocument(
                new ContentBlock.Image(1L, false, null, null, null, null)
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(mediaDocument(
                new ContentBlock.Image(1L, true, "not decorative", null, null, null)
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(mediaDocument(
                new ContentBlock.Image(1L, false, "x".repeat(301), null, null, null)
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(mediaDocument(
                new ContentBlock.Image(1L, false, "Safe", text("x".repeat(1001)), null, null)
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(mediaDocument(
                new ContentBlock.Image(1L, false, "Safe", null, null,
                        new ContentLink("javascript:alert(1)", false, false, false))
        )), "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void normalizesApprovedYouTubeFormsAndRejectsLookalikesUnknownProvidersAndV1Media() {
        for (String source : List.of(
                "dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ",
                "https://youtube.com/shorts/dQw4w9WgXcQ"
        )) {
            ContentDocument normalized = validator.validateAndNormalize(mediaDocument(
                    new ContentBlock.Embed(EmbedProvider.YOUTUBE, source, null)
            ));
            assertThat(((ContentBlock.Embed) normalized.blocks().get(0)).externalId())
                    .isEqualTo("dQw4w9WgXcQ");
        }
        for (String unsafe : List.of(
                "https://youtube.example.com/watch?v=dQw4w9WgXcQ",
                "https://evil-youtube.com/watch?v=dQw4w9WgXcQ",
                "javascript:dQw4w9WgXcQ",
                "data:dQw4w9WgXcQ"
        )) {
            assertCode(() -> validator.validateAndNormalize(mediaDocument(
                    new ContentBlock.Embed(EmbedProvider.YOUTUBE, unsafe, null)
            )), "CONTENT_DOCUMENT_INVALID");
        }
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(1, List.of(
                new ContentBlock.Image(1L, false, "Image", null, null, null)
        ))), "CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED");
    }

    @Test
    void versionOneTextIsReadableAndUpcastToCurrentVersionOnValidation() {
        ContentDocument normalized = validator.validateAndNormalize(new ContentDocument(1, List.of(
                new ContentBlock.Paragraph(text("Existing post"))
        )));
        assertThat(normalized.schemaVersion()).isEqualTo(ContentDocument.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void versionTwoDocumentsAreReadableAndUpcastToCurrentVersionOnValidation() {
        ContentDocument normalized = validator.validateAndNormalize(mediaDocument(
                new ContentBlock.Image(1L, false, "Existing image", null, null, null)
        ));
        assertThat(normalized.schemaVersion()).isEqualTo(ContentDocument.CURRENT_SCHEMA_VERSION);
    }

    // ── CHECK_LIST ────────────────────────────────────────────────────────────

    @Test
    void checkListAcceptsSingleAndManyItemsWithMarksAndSafeLinksAndRoundTrips() throws Exception {
        ContentDocument document = v3Document(new ContentBlock.CheckList(List.of(
                new ContentBlock.ListItem(text("Earthquake-resistant RCC frame structure")),
                new ContentBlock.ListItem(List.of(
                        new InlineNode.Text("Imported ", List.of()),
                        new InlineNode.Text("marble flooring", List.of(new TextMark.Bold(),
                                new TextMark.Link("https://squarefootstory.com/finishes", false, false, false)))
                ))
        )));

        ContentDocument normalized = validator.validateAndNormalize(document);
        ContentBlock.CheckList checkList = (ContentBlock.CheckList) normalized.blocks().get(0);
        assertThat(checkList.items()).hasSize(2);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                .isEqualTo(normalized);

        int items = ContentDocumentLimits.MAX_LIST_ITEMS;
        ContentDocument atMax = v3Document(new ContentBlock.CheckList(
                java.util.Collections.nCopies(items, new ContentBlock.ListItem(text("Verified")))
        ));
        assertThat(validator.validateAndNormalize(atMax)).isNotNull();

        ContentDocument overMax = v3Document(new ContentBlock.CheckList(
                java.util.Collections.nCopies(items + 1, new ContentBlock.ListItem(text("Verified")))
        ));
        assertCode(() -> validator.validateAndNormalize(overMax), "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void checkListRejectsEmptyListAndBlankItems() {
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.CheckList(List.of()))),
                "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.CheckList(List.of(
                new ContentBlock.ListItem(List.of(new InlineNode.Text("   ", List.of())))
        )))), "CONTENT_DOCUMENT_INVALID");
    }

    // ── CALLOUT ───────────────────────────────────────────────────────────────

    @Test
    void calloutAcceptsAllVariantsWithAndWithoutTitleAndRoundTrips() throws Exception {
        for (CalloutVariant variant : CalloutVariant.values()) {
            ContentDocument withTitle = v3Document(
                    new ContentBlock.Callout(variant, "SFS Verdict", text("Strong fundamentals overall."))
            );
            ContentDocument normalized = validator.validateAndNormalize(withTitle);
            ContentBlock.Callout callout = (ContentBlock.Callout) normalized.blocks().get(0);
            assertThat(callout.variant()).isEqualTo(variant);
            assertThat(callout.title()).isEqualTo("SFS Verdict");
            assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                    .isEqualTo(normalized);

            ContentDocument withoutTitle = v3Document(
                    new ContentBlock.Callout(variant, null, text("No hard-coded title required."))
            );
            ContentBlock.Callout noTitle = (ContentBlock.Callout) validator.validateAndNormalize(withoutTitle).blocks().get(0);
            assertThat(noTitle.title()).isNull();
        }
    }

    @Test
    void calloutRequiresVariantAndVisibleContentAndEnforcesTitleLimit() {
        assertCode(() -> validator.validateAndNormalize(v3Document(
                new ContentBlock.Callout(null, "Title", text("Body"))
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(
                new ContentBlock.Callout(CalloutVariant.INFO, "Title", List.of())
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(
                new ContentBlock.Callout(CalloutVariant.INFO, "Title", text("   "))
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Callout(
                CalloutVariant.INFO,
                "x".repeat(ContentDocumentLimits.MAX_CALLOUT_TITLE_CHARACTERS + 1),
                text("Body")
        ))), "CONTENT_DOCUMENT_INVALID");
        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Callout(
                CalloutVariant.INFO,
                "x".repeat(ContentDocumentLimits.MAX_CALLOUT_TITLE_CHARACTERS),
                text("Body")
        )))).isNotNull();
    }

    @Test
    void calloutUnknownVariantFailsJsonParsingSafely() {
        String json = documentJson("""
                {"type":"CALLOUT","variant":"DANGER","title":null,"content":[{"type":"TEXT","text":"x","marks":[]}]}
                """);
        assertThatThrownBy(() -> objectMapper.readValue(json, ContentDocument.class)).isInstanceOf(Exception.class);
    }

    // ── TABLE ─────────────────────────────────────────────────────────────────

    @Test
    void tableAcceptsSimpleTwoColumnProjectSnapshotAndRoundTrips() throws Exception {
        ContentDocument document = v3Document(new ContentBlock.Table(
                "Project Snapshot",
                List.of(new ContentBlock.TableColumn("Package"), new ContentBlock.TableColumn("Estimated Cost")),
                List.of(
                        new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Price"), text("₹2.20 – 3.93 Cr"))),
                        new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Configuration"), text("2, 3 & 4 BHK"))),
                        new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Towers"), text("7")))
                )
        ));

        ContentDocument normalized = validator.validateAndNormalize(document);
        ContentBlock.Table table = (ContentBlock.Table) normalized.blocks().get(0);
        assertThat(table.caption()).isEqualTo("Project Snapshot");
        assertThat(table.columns()).hasSize(2);
        assertThat(table.rows()).hasSize(3);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                .isEqualTo(normalized);
    }

    @Test
    void tableSupportsSectionAndTotalRowsForMultiPartBudgetLayout() {
        ContentDocument document = v3Document(new ContentBlock.Table(
                "Luxury Interior & Move-in Budget Estimate",
                List.of(new ContentBlock.TableColumn("Package"), new ContentBlock.TableColumn("Estimated Cost")),
                List.of(
                        new ContentBlock.TableRow(TableRowType.SECTION, List.of(text("Interior Packages"))),
                        new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Premium"), text("₹20–25 lakh"))),
                        new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Luxury"), text("₹35–45 lakh"))),
                        new ContentBlock.TableRow(TableRowType.SECTION, List.of(text("Estimated Total Move-in Budget"))),
                        new ContentBlock.TableRow(TableRowType.TOTAL, List.of(text("Apartment"), text("₹2–3 Crore")))
                )
        ));

        ContentDocument normalized = validator.validateAndNormalize(document);
        ContentBlock.Table table = (ContentBlock.Table) normalized.blocks().get(0);
        assertThat(table.rows().get(0).rowType()).isEqualTo(TableRowType.SECTION);
        assertThat(table.rows().get(0).cells()).hasSize(1);
        assertThat(table.rows().get(4).rowType()).isEqualTo(TableRowType.TOTAL);
        assertThat(table.rows().get(4).cells()).hasSize(2);
    }

    @Test
    void tableEnforcesColumnAndRowLimitsAtBoundaryAndOneOver() {
        List<ContentBlock.TableColumn> maxColumns = new ArrayList<>();
        List<List<InlineNode>> maxCells = new ArrayList<>();
        for (int i = 0; i < ContentDocumentLimits.MAX_TABLE_COLUMNS; i++) {
            maxColumns.add(new ContentBlock.TableColumn("Col " + i));
            maxCells.add(text("v" + i));
        }
        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Table(
                null, maxColumns, List.of(new ContentBlock.TableRow(TableRowType.NORMAL, maxCells))
        )))).isNotNull();

        List<ContentBlock.TableColumn> overColumns = new ArrayList<>(maxColumns);
        overColumns.add(new ContentBlock.TableColumn("Extra"));
        List<List<InlineNode>> overCells = new ArrayList<>(maxCells);
        overCells.add(text("extra"));
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(
                new ContentBlock.Table(null, overColumns, List.of(new ContentBlock.TableRow(TableRowType.NORMAL, overCells)))
        ))), "CONTENT_DOCUMENT_INVALID");

        List<ContentBlock.TableColumn> twoColumns = List.of(
                new ContentBlock.TableColumn("A"), new ContentBlock.TableColumn("B")
        );
        List<ContentBlock.TableRow> maxRows = new ArrayList<>();
        for (int i = 0; i < ContentDocumentLimits.MAX_TABLE_ROWS; i++) {
            maxRows.add(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("k" + i), text("v" + i))));
        }
        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Table(null, twoColumns, maxRows))))
                .isNotNull();

        List<ContentBlock.TableRow> overRows = new ArrayList<>(maxRows);
        overRows.add(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("extra"), text("extra"))));
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Table(null, twoColumns, overRows))),
                "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void tableRejectsCellCountMismatchForNormalAndSectionRows() {
        List<ContentBlock.TableColumn> twoColumns = List.of(
                new ContentBlock.TableColumn("A"), new ContentBlock.TableColumn("B")
        );
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Table(
                null, twoColumns, List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("only one"))))
        ))), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Table(
                null, twoColumns, List.of(new ContentBlock.TableRow(
                        TableRowType.SECTION, List.of(text("one"), text("two"))
                ))
        ))), "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void tableRequiresAtLeastOneColumnRowAndVisibleCellText() {
        assertCode(() -> validator.validateAndNormalize(v3Document(
                new ContentBlock.Table(null, List.of(), List.of())
        )), "CONTENT_DOCUMENT_INVALID");
        List<ContentBlock.TableColumn> oneColumn = List.of(new ContentBlock.TableColumn("A"));
        assertCode(() -> validator.validateAndNormalize(v3Document(
                new ContentBlock.Table(null, oneColumn, List.of())
        )), "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Table(
                null, oneColumn, List.of(new ContentBlock.TableRow(
                        TableRowType.NORMAL, List.of(List.of(new InlineNode.Text("   ", List.of())))
                ))
        ))), "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void tableCellsSupportInlineMarksButNoNestedBlocks() throws Exception {
        List<ContentBlock.TableColumn> oneColumn = List.of(new ContentBlock.TableColumn("Detail"));
        ContentDocument document = v3Document(new ContentBlock.Table(
                null, oneColumn, List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(
                        new InlineNode.Text("Read the ", List.of()),
                        new InlineNode.Text("brochure", List.of(new TextMark.Bold(),
                                new TextMark.Link("https://squarefootstory.com/brochure", true, false, false)))
                ))))
        ));
        ContentDocument normalized = validator.validateAndNormalize(document);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                .isEqualTo(normalized);
        // No IMAGE/VIDEO/TABLE JsonSubTypes exist for the cell's InlineNode list, so nesting a
        // block inside a cell is a compile-time impossibility, not merely a runtime rejection.
    }

    // ── TABLE title (v4) ────────────────────────────────────────────────────────

    @Test
    void tableTitleIsOptionalDistinctFromCaptionAndBoundedLikeOtherLabels() {
        List<ContentBlock.TableColumn> oneColumn = List.of(new ContentBlock.TableColumn("A"));
        List<ContentBlock.TableRow> oneRow = List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("v"))));

        ContentDocument normalized = validator.validateAndNormalize(v3Document(
                new ContentBlock.Table("Interior Packages", "Prices as of 2026", oneColumn, oneRow)
        ));
        ContentBlock.Table table = (ContentBlock.Table) normalized.blocks().get(0);
        assertThat(table.title()).isEqualTo("Interior Packages");
        assertThat(table.caption()).isEqualTo("Prices as of 2026");

        // Pre-v4 3-arg construction still works and leaves title null.
        assertThat(((ContentBlock.Table) validator.validateAndNormalize(
                v3Document(new ContentBlock.Table(null, oneColumn, oneRow))
        ).blocks().get(0)).title()).isNull();

        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Table(
                "x".repeat(ContentDocumentLimits.MAX_TABLE_TITLE_CHARACTERS), null, oneColumn, oneRow
        )))).isNotNull();
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Table(
                "x".repeat(ContentDocumentLimits.MAX_TABLE_TITLE_CHARACTERS + 1), null, oneColumn, oneRow
        ))), "CONTENT_DOCUMENT_INVALID");
    }

    // ── LAYOUT (v4) ──────────────────────────────────────────────────────────────

    private ContentBlock.Image image(String alt) {
        return new ContentBlock.Image(1L, false, alt, List.of(), ImageLayout.STANDARD, null);
    }

    private ContentBlock.Table oneCellTable(String title) {
        return new ContentBlock.Table(
                title, null,
                List.of(new ContentBlock.TableColumn("A")),
                List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("v"))))
        );
    }

    @Test
    void layoutAcceptsTwoAndThreeImageChildrenAndRoundTrips() throws Exception {
        for (int n : List.of(2, 3)) {
            List<LayoutChildBlock> children = new ArrayList<>();
            for (int i = 0; i < n; i++) children.add(image("Image " + i));
            ContentDocument document = v3Document(new ContentBlock.Layout(n, children));

            ContentDocument normalized = validator.validateAndNormalize(document);
            ContentBlock.Layout layout = (ContentBlock.Layout) normalized.blocks().get(0);
            assertThat(layout.columns()).isEqualTo(n);
            assertThat(layout.children()).hasSize(n);
            assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                    .isEqualTo(normalized);
        }
    }

    @Test
    void layoutAcceptsTwoAndThreeTableChildrenAndRoundTrips() throws Exception {
        for (int n : List.of(2, 3)) {
            List<LayoutChildBlock> children = new ArrayList<>();
            for (int i = 0; i < n; i++) children.add(oneCellTable("Table " + i));
            ContentDocument document = v3Document(new ContentBlock.Layout(n, children));

            ContentDocument normalized = validator.validateAndNormalize(document);
            ContentBlock.Layout layout = (ContentBlock.Layout) normalized.blocks().get(0);
            assertThat(layout.children()).hasSize(n);
            assertThat(layout.children()).allMatch(ContentBlock.Table.class::isInstance);
            assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                    .isEqualTo(normalized);
        }
    }

    @Test
    void layoutAcceptsMixedImageAndTableChildrenInEitherOrder() {
        ContentDocument imageThenTable = v3Document(new ContentBlock.Layout(2, List.of(
                image("Alpha Tower"), oneCellTable("Cost")
        )));
        ContentDocument tableThenImage = v3Document(new ContentBlock.Layout(2, List.of(
                oneCellTable("Cost"), image("Alpha Tower")
        )));

        assertThat(((ContentBlock.Layout) validator.validateAndNormalize(imageThenTable).blocks().get(0)).children())
                .hasSize(2)
                .satisfiesExactly(
                        c -> assertThat(c).isInstanceOf(ContentBlock.Image.class),
                        c -> assertThat(c).isInstanceOf(ContentBlock.Table.class)
                );
        assertThat(((ContentBlock.Layout) validator.validateAndNormalize(tableThenImage).blocks().get(0)).children())
                .hasSize(2)
                .satisfiesExactly(
                        c -> assertThat(c).isInstanceOf(ContentBlock.Table.class),
                        c -> assertThat(c).isInstanceOf(ContentBlock.Image.class)
                );
    }

    @Test
    void layoutChildCountExceedingColumnsIsValidAndAutoWrapsAtRenderTimeNotHere() {
        // 8 children in a 3-column layout is exactly the CSS-grid auto-wrap scenario (Step 12) —
        // the validator enforces no columns/children relationship at all, only the independent
        // columns range and the overall children-count cap.
        List<LayoutChildBlock> eightChildren = new ArrayList<>();
        for (int i = 0; i < 8; i++) eightChildren.add(image("Image " + i));

        ContentDocument normalized = validator.validateAndNormalize(v3Document(new ContentBlock.Layout(3, eightChildren)));
        assertThat(((ContentBlock.Layout) normalized.blocks().get(0)).children()).hasSize(8);
    }

    @Test
    void layoutRejectsColumnsOutsideOneToThree() {
        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Layout(1, List.of(image("A")))))).isNotNull();
        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Layout(3, List.of(image("A")))))).isNotNull();
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Layout(0, List.of(image("A"))))),
                "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Layout(4, List.of(image("A"))))),
                "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void layoutRejectsEmptyOrNullChildren() {
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Layout(2, List.of()))),
                "CONTENT_DOCUMENT_INVALID");
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Layout(2, null))),
                "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void layoutEnforcesMaxChildrenAtBoundaryAndOneOver() {
        List<LayoutChildBlock> maxChildren = new ArrayList<>();
        for (int i = 0; i < ContentDocumentLimits.MAX_LAYOUT_CHILDREN; i++) maxChildren.add(image("Image " + i));
        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Layout(3, maxChildren)))).isNotNull();

        List<LayoutChildBlock> overChildren = new ArrayList<>(maxChildren);
        overChildren.add(image("One too many"));
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Layout(3, overChildren))),
                "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void layoutChildrenAreValidatedUnderTheExactSameRulesAsTopLevelImageAndTable() {
        // A non-decorative image with no alt text is invalid at top level - it must be exactly
        // as invalid as a layout child, never a looser nested path (Step 16).
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Layout(
                2, List.of(new ContentBlock.Image(1L, false, null, List.of(), ImageLayout.STANDARD, null), image("ok"))
        ))), "CONTENT_DOCUMENT_INVALID");
        // A table with no visible cell text is invalid at top level - same for a layout child.
        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Layout(
                2, List.of(new ContentBlock.Table(
                        null, null, List.of(new ContentBlock.TableColumn("A")),
                        List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(new InlineNode.Text("  ", List.of())))))
                ), image("ok"))
        ))), "CONTENT_DOCUMENT_INVALID");
    }

    @Test
    void layoutNestedInsideLayoutIsUnrepresentableAtJsonParsingLevel() {
        // LayoutChildBlock's own @JsonSubTypes registers only IMAGE/TABLE (see LayoutChildBlock) -
        // "type":"LAYOUT" inside a layout's children array has no matching subtype and fails to
        // deserialize at all. This is a compile-time/schema fact, not a validator rule that a
        // future edit could accidentally loosen.
        String json = """
                {"schemaVersion":4,"blocks":[
                    {"type":"LAYOUT","columns":2,"children":[
                        {"type":"LAYOUT","columns":2,"children":[
                            {"type":"IMAGE","mediaAssetId":1,"decorative":true,"altText":null,"caption":[],"layout":"STANDARD","link":null}
                        ]},
                        {"type":"IMAGE","mediaAssetId":2,"decorative":true,"altText":null,"caption":[],"layout":"STANDARD","link":null}
                    ]}
                ]}
                """;
        assertThatThrownBy(() -> objectMapper.readValue(json, ContentDocument.class)).isInstanceOf(Exception.class);
    }

    @Test
    void layoutRequiresSchemaVersionFourAndOlderDocumentsRejectIt() {
        assertCode(() -> validator.validateAndNormalize(new ContentDocument(3, List.of(
                new ContentBlock.Layout(2, List.of(image("A"), image("B")))
        ))), "CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED");
        // v4 (CURRENT_SCHEMA_VERSION) accepts it.
        assertThat(validator.validateAndNormalize(v3Document(new ContentBlock.Layout(2, List.of(image("A"), image("B"))))))
                .isNotNull();
    }

    @Test
    void preV4DocumentsWithoutLayoutRemainValidAndUnaffected() throws Exception {
        // A pre-existing v2 IMAGE-only document (no LAYOUT anywhere) must keep validating and
        // normalizing exactly as before this feature (Step 33: no automatic reinterpretation of
        // consecutive standalone blocks as a grid).
        ContentDocument document = mediaDocument(new ContentBlock.Image(
                1L, false, "Standalone image", List.of(), ImageLayout.STANDARD, null
        ));
        ContentDocument normalized = validator.validateAndNormalize(document);
        assertThat(normalized.schemaVersion()).isEqualTo(ContentDocument.CURRENT_SCHEMA_VERSION);
        assertThat(normalized.blocks()).hasSize(1).allMatch(ContentBlock.Image.class::isInstance);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                .isEqualTo(normalized);
    }

    @Test
    void layoutContributesToWordCountThroughItsChildrenExactlyLikeTopLevelBlocks() {
        ContentDocument standalone = v3Document(image("caption text ignored for image alt"), oneCellTable("Title Words Here"));
        ContentDocument insideLayout = v3Document(new ContentBlock.Layout(2, List.of(
                image("caption text ignored for image alt"), oneCellTable("Title Words Here")
        )));

        int standaloneCount = new ContentDocumentWordCounter().count(validator.validateAndNormalize(standalone));
        int layoutCount = new ContentDocumentWordCounter().count(validator.validateAndNormalize(insideLayout));

        // Image alt text is not counted (matches top-level behavior - only caption/table text
        // contribute); the table's title/cell text does, identically whether nested or not.
        assertThat(layoutCount).isEqualTo(standaloneCount);
        assertThat(layoutCount).isGreaterThan(0);
    }

    // ── Security (reuses the same safe-plain-text / link-safety pipeline) ─────

    @Test
    void newBlocksRejectUnsafeLinksAndStoreMarkupLookingTextAsPlainTextForRendererEscaping() {
        String unsafeHtml = "<script>alert(1)</script><img onerror=alert(1)>";

        ContentDocument callout = validator.validateAndNormalize(v3Document(
                new ContentBlock.Callout(CalloutVariant.WARNING, null, List.of(new InlineNode.Text(unsafeHtml, List.of())))
        ));
        assertThat(((InlineNode.Text) ((ContentBlock.Callout) callout.blocks().get(0)).content().get(0)).text())
                .isEqualTo(unsafeHtml);

        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Callout(
                CalloutVariant.WARNING, null,
                List.of(new InlineNode.Text("click", List.of(new TextMark.Link("javascript:alert(1)", false, false, false))))
        ))), "CONTENT_DOCUMENT_INVALID");

        assertCode(() -> validator.validateAndNormalize(v3Document(new ContentBlock.Table(
                null, List.of(new ContentBlock.TableColumn("Link")),
                List.of(new ContentBlock.TableRow(TableRowType.NORMAL, List.of(List.of(
                        new InlineNode.Text("click", List.of(new TextMark.Link("data:text/html,boom", false, false, false)))
                ))))
        ))), "CONTENT_DOCUMENT_INVALID");
    }

    // ── Realistic v3 article fixture ────────────────────────────────────────────

    @Test
    void realisticV3ArticleFixtureValidatesAndRoundTrips() throws Exception {
        ContentDocument article = v3Document(
                new ContentBlock.Heading(HeadingLevel.H2, text("Project Overview")),
                new ContentBlock.Paragraph(List.of(
                        new InlineNode.Text("This synthetic listing sits on the ", List.of()),
                        new InlineNode.Text("Dwarka Expressway", List.of(new TextMark.Bold())),
                        new InlineNode.Text(" corridor.", List.of())
                )),
                new ContentBlock.Heading(HeadingLevel.H2, text("Construction Specifications")),
                new ContentBlock.CheckList(List.of(
                        new ContentBlock.ListItem(text("Earthquake-resistant RCC frame structure")),
                        new ContentBlock.ListItem(text("Imported marble flooring")),
                        new ContentBlock.ListItem(text("Smart switches")),
                        new ContentBlock.ListItem(text("UPVC windows"))
                )),
                new ContentBlock.Heading(HeadingLevel.H2, text("Project Snapshot")),
                new ContentBlock.Table(
                        "Project Snapshot",
                        List.of(new ContentBlock.TableColumn("Package"), new ContentBlock.TableColumn("Estimated Cost")),
                        List.of(
                                new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Price"), text("₹2.20 – 3.93 Cr"))),
                                new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Configuration"), text("2, 3 & 4 BHK"))),
                                new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Possession"), text("November 2030"))),
                                new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Towers"), text("7")))
                        )
                ),
                new ContentBlock.Table(
                        "Luxury Interior & Move-in Budget Estimate",
                        List.of(new ContentBlock.TableColumn("Package"), new ContentBlock.TableColumn("Estimated Cost")),
                        List.of(
                                new ContentBlock.TableRow(TableRowType.SECTION, List.of(text("Interior Packages"))),
                                new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Premium"), text("₹20–25 lakh"))),
                                new ContentBlock.TableRow(TableRowType.NORMAL, List.of(text("Luxury"), text("₹35–45 lakh"))),
                                new ContentBlock.TableRow(TableRowType.SECTION, List.of(text("Estimated Total Move-in Budget"))),
                                new ContentBlock.TableRow(TableRowType.TOTAL, List.of(text("Apartment"), text("₹2–3 Crore")))
                        )
                ),
                new ContentBlock.Divider(),
                new ContentBlock.Callout(
                        CalloutVariant.VERDICT,
                        "SFS Verdict",
                        text("For buyers looking to benefit from strong connectivity and pricing, this is a compelling option.")
                )
        );

        ContentDocument normalized = validator.validateAndNormalize(article);
        assertThat(normalized.schemaVersion()).isEqualTo(ContentDocument.CURRENT_SCHEMA_VERSION);
        assertThat(normalized.blocks()).hasSize(9);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsBytes(normalized), ContentDocument.class))
                .isEqualTo(normalized);
        assertThat(new ContentDocumentWordCounter().count(normalized)).isGreaterThan(0);
    }

    private ContentDocument v3Document(ContentBlock... blocks) {
        return new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(blocks));
    }

    private List<InlineNode> text(String value) {
        return List.of(new InlineNode.Text(value, List.of()));
    }

    private ContentDocument documentWithLink(String href) {
        return new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(new ContentBlock.Paragraph(List.of(
                new InlineNode.Text("link", List.of(new TextMark.Link(href, false, false, false)))
        ))));
    }

    private ContentDocument mediaDocument(ContentBlock block) {
        return new ContentDocument(2, List.of(block));
    }

    private String documentJson(String block) {
        return "{\"schemaVersion\":1,\"blocks\":[" + block + "]}";
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable)
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo(code);
    }
}
