package com.brandPitara.sfs.migration.wordpress.gutenberg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GutenbergBlockParserTest {

    private final GutenbergBlockParser parser = new GutenbergBlockParser();

    @Test
    void parsesASimpleLeafBlock() {
        List<GutenbergBlock> blocks = parser.parse("""
                <!-- wp:paragraph -->
                <p>Hello world</p>
                <!-- /wp:paragraph -->
                """);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).blockName()).isEqualTo("paragraph");
        assertThat(blocks.get(0).normalizedBlockName()).isEqualTo("core/paragraph");
        assertThat(blocks.get(0).innerHtml()).contains("<p>Hello world</p>");
        assertThat(blocks.get(0).children()).isEmpty();
    }

    @Test
    void parsesAttributesAsJson() {
        List<GutenbergBlock> blocks = parser.parse(
                "<!-- wp:image {\"id\":123,\"sizeSlug\":\"large\"} -->\n<figure></figure>\n<!-- /wp:image -->"
        );
        GutenbergBlock image = blocks.get(0);
        assertThat(image.attributes().get("id").asInt()).isEqualTo(123);
        assertThat(image.attributes().get("sizeSlug").asText()).isEqualTo("large");
    }

    @Test
    void parsesNestedAttributesJsonWithInternalBraces() {
        List<GutenbergBlock> blocks = parser.parse(
                "<!-- wp:group {\"style\":{\"spacing\":{\"padding\":{\"top\":\"20px\"}}}} -->\n<div></div>\n<!-- /wp:group -->"
        );
        GutenbergBlock group = blocks.get(0);
        assertThat(group.attributes().at("/style/spacing/padding/top").asText()).isEqualTo("20px");
    }

    @Test
    void parsesSelfClosingBlockWithNoChildren() {
        List<GutenbergBlock> blocks = parser.parse("<!-- wp:separator /-->");
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).blockName()).isEqualTo("separator");
        assertThat(blocks.get(0).innerHtml()).isEmpty();
        assertThat(blocks.get(0).children()).isEmpty();
    }

    @Test
    void contentAfterASelfClosingBlockIsSeparateFreeformContentNotAbsorbedIntoIt() {
        List<GutenbergBlock> blocks = parser.parse("<!-- wp:separator /-->\n<hr/>");
        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).blockName()).isEqualTo("separator");
        assertThat(blocks.get(1).isFreeform()).isTrue();
        assertThat(blocks.get(1).innerHtml()).contains("<hr/>");
    }

    @Test
    void parsesSelfClosingBlockWithAttributes() {
        List<GutenbergBlock> blocks = parser.parse("<!-- wp:spacer {\"height\":\"40px\"} /-->");
        assertThat(blocks.get(0).attributes().get("height").asText()).isEqualTo("40px");
    }

    @Test
    void parsesNestedColumnsColumnParagraphPreservingStructureAndOrder() {
        List<GutenbergBlock> blocks = parser.parse("""
                <!-- wp:columns -->
                <div class="wp-block-columns">
                <!-- wp:column -->
                <div class="wp-block-column"><!-- wp:paragraph -->
                <p>First</p>
                <!-- /wp:paragraph --></div>
                <!-- /wp:column -->

                <!-- wp:column -->
                <div class="wp-block-column"><!-- wp:paragraph -->
                <p>Second</p>
                <!-- /wp:paragraph --></div>
                <!-- /wp:column -->
                </div>
                <!-- /wp:columns -->
                """);

        assertThat(blocks).hasSize(1);
        GutenbergBlock columns = blocks.get(0);
        assertThat(columns.blockName()).isEqualTo("columns");
        assertThat(columns.children()).hasSize(2);
        assertThat(columns.children().get(0).blockName()).isEqualTo("column");
        assertThat(columns.children().get(0).children()).hasSize(1);
        assertThat(columns.children().get(0).children().get(0).innerHtml()).contains("First");
        assertThat(columns.children().get(1).children().get(0).innerHtml()).contains("Second");
    }

    @Test
    void preservesTopLevelBlockOrder() {
        List<GutenbergBlock> blocks = parser.parse("""
                <!-- wp:heading -->
                <h2>A</h2>
                <!-- /wp:heading -->
                <!-- wp:paragraph -->
                <p>B</p>
                <!-- /wp:paragraph -->
                <!-- wp:separator /-->
                <!-- wp:paragraph -->
                <p>C</p>
                <!-- /wp:paragraph -->
                """);
        assertThat(blocks).extracting(GutenbergBlock::blockName)
                .containsExactly("heading", "paragraph", "separator", "paragraph");
    }

    @Test
    void capturesFreeformHtmlOutsideAnyBlockComment() {
        List<GutenbergBlock> blocks = parser.parse("<p>Classic editor content</p>");
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).isFreeform()).isTrue();
        assertThat(blocks.get(0).innerHtml()).isEqualTo("<p>Classic editor content</p>");
    }

    @Test
    void ignoresWhitespaceOnlyGapsBetweenBlocksAsFreeform() {
        List<GutenbergBlock> blocks = parser.parse("""
                <!-- wp:paragraph -->
                <p>A</p>
                <!-- /wp:paragraph -->

                <!-- wp:paragraph -->
                <p>B</p>
                <!-- /wp:paragraph -->
                """);
        assertThat(blocks).extracting(GutenbergBlock::blockName).containsExactly("paragraph", "paragraph");
    }

    @Test
    void thirdPartyBlockNameKeepsItsOwnNamespace() {
        List<GutenbergBlock> blocks = parser.parse(
                "<!-- wp:feedbackwp/rating-widget /-->"
        );
        assertThat(blocks.get(0).blockName()).isEqualTo("feedbackwp/rating-widget");
        assertThat(blocks.get(0).normalizedBlockName()).isEqualTo("feedbackwp/rating-widget");
    }

    @Test
    void ordinaryHtmlCommentIsNotMistakenForABlockDelimiter() {
        List<GutenbergBlock> blocks = parser.parse("<!-- just a regular comment --><p>Text</p>");
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).isFreeform()).isTrue();
        assertThat(blocks.get(0).innerHtml()).contains("just a regular comment").contains("<p>Text</p>");
    }

    @Test
    void malformedUnclosedBlockAtEndOfContentIsForceClosedRatherThanLosingContent() {
        List<GutenbergBlock> blocks = parser.parse("<!-- wp:paragraph -->\n<p>Never closed</p>");
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).blockName()).isEqualTo("paragraph");
        assertThat(blocks.get(0).innerHtml()).contains("Never closed");
    }

    @Test
    void malformedAttributesJsonFallsBackToEmptyObjectRatherThanThrowing() {
        List<GutenbergBlock> blocks = parser.parse(
                "<!-- wp:image {not valid json} -->\n<figure></figure>\n<!-- /wp:image -->"
        );
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).attributes().isObject()).isTrue();
        assertThat(blocks.get(0).attributes().isEmpty()).isTrue();
    }

    @Test
    void unicodeAndHtmlEntitiesInsideBlockContentSurviveIntact() {
        String text = "Gurgaon – café living 🏠 &amp; more";
        List<GutenbergBlock> blocks = parser.parse(
                "<!-- wp:paragraph -->\n<p>" + text + "</p>\n<!-- /wp:paragraph -->"
        );
        assertThat(blocks.get(0).innerHtml()).contains(text);
    }
}
