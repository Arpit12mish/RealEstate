package com.brandPitara.sfs.cms.content.document;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ContentBlock.Paragraph.class, name = "PARAGRAPH"),
        @JsonSubTypes.Type(value = ContentBlock.Heading.class, name = "HEADING"),
        @JsonSubTypes.Type(value = ContentBlock.BulletList.class, name = "BULLET_LIST"),
        @JsonSubTypes.Type(value = ContentBlock.OrderedList.class, name = "ORDERED_LIST"),
        @JsonSubTypes.Type(value = ContentBlock.Blockquote.class, name = "BLOCKQUOTE"),
        @JsonSubTypes.Type(value = ContentBlock.Divider.class, name = "DIVIDER"),
        @JsonSubTypes.Type(value = ContentBlock.Image.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = ContentBlock.Video.class, name = "VIDEO"),
        @JsonSubTypes.Type(value = ContentBlock.Embed.class, name = "EMBED"),
        @JsonSubTypes.Type(value = ContentBlock.CheckList.class, name = "CHECK_LIST"),
        @JsonSubTypes.Type(value = ContentBlock.Callout.class, name = "CALLOUT"),
        @JsonSubTypes.Type(value = ContentBlock.Table.class, name = "TABLE"),
        @JsonSubTypes.Type(value = ContentBlock.Layout.class, name = "LAYOUT")
})
public sealed interface ContentBlock permits
        ContentBlock.Paragraph,
        ContentBlock.Heading,
        ContentBlock.BulletList,
        ContentBlock.OrderedList,
        ContentBlock.Blockquote,
        ContentBlock.Divider,
        ContentBlock.Image,
        ContentBlock.Video,
        ContentBlock.Embed,
        ContentBlock.CheckList,
        ContentBlock.Callout,
        ContentBlock.Table,
        ContentBlock.Layout {

    @JsonAnySetter
    default void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException("Unknown block property: " + property);
    }

    @JsonTypeName("PARAGRAPH")
    record Paragraph(List<InlineNode> content) implements ContentBlock {
    }

    @JsonTypeName("HEADING")
    record Heading(HeadingLevel level, List<InlineNode> content) implements ContentBlock {
    }

    @JsonTypeName("BULLET_LIST")
    record BulletList(List<ListItem> items) implements ContentBlock {
    }

    @JsonTypeName("ORDERED_LIST")
    record OrderedList(List<ListItem> items) implements ContentBlock {
    }

    @JsonTypeName("BLOCKQUOTE")
    record Blockquote(List<InlineNode> content) implements ContentBlock {
    }

    @JsonTypeName("DIVIDER")
    record Divider() implements ContentBlock {
    }

    @JsonTypeName("IMAGE")
    record Image(
            Long mediaAssetId,
            boolean decorative,
            String altText,
            List<InlineNode> caption,
            ImageLayout layout,
            ContentLink link
    ) implements ContentBlock, LayoutChildBlock {
    }

    @JsonTypeName("VIDEO")
    record Video(
            Long mediaAssetId,
            Long posterMediaAssetId,
            List<InlineNode> caption
    ) implements ContentBlock {
    }

    @JsonTypeName("EMBED")
    record Embed(
            EmbedProvider provider,
            String externalId,
            List<InlineNode> caption
    ) implements ContentBlock {
    }

    record ListItem(List<InlineNode> content) {
        @JsonAnySetter
        public void rejectUnknownProperty(String property, Object ignored) {
            throw new IllegalArgumentException("Unknown list-item property: " + property);
        }
    }

    /**
     * Editorial "verified/features" checklist (construction specs, nearby landmarks, etc.),
     * not an interactive todo list — items carry no checked/unchecked state. Reuses
     * {@link ListItem} directly since the shape and validation rules are identical to
     * BULLET_LIST/ORDERED_LIST items.
     */
    @JsonTypeName("CHECK_LIST")
    record CheckList(List<ListItem> items) implements ContentBlock {
    }

    /**
     * Controlled editorial box (SFS Verdict, Editor's Note, etc.). {@code variant} maps to a
     * renderer-owned appearance — no colors/CSS are persisted. {@code title} is optional and
     * never defaulted server-side (e.g. VERDICT's "SFS Verdict" label is a dashboard/renderer
     * presentation concern, not backend logic).
     */
    @JsonTypeName("CALLOUT")
    record Callout(CalloutVariant variant, String title, List<InlineNode> content) implements ContentBlock {
    }

    /**
     * Structured semantic table. The header row is {@link TableColumn#label()} for every
     * column, not a row type, so there is exactly one way to express "this is the header".
     * A SECTION row is a single full-width label cell (semantically "spans the table")
     * rather than a stored colSpan/rowSpan number. Cells reuse {@link InlineNode} directly
     * (no cell wrapper record) so images/videos/tables inside a cell are not merely
     * disallowed by validation — they are unrepresentable in the type system.
     * <p>
     * {@code title}, added alongside LAYOUT (v4), is a distinct concept from {@code caption}:
     * a short heading rendered ABOVE the table (e.g. a cost-card's "Interior Packages"), while
     * {@code caption} remains the existing small annotation rendered below. Optional and
     * defaulted to {@code null} so every pre-v4 document round-trips unchanged. The convenience
     * 3-arg constructor preserves every existing call site that predates this field.
     */
    @JsonTypeName("TABLE")
    record Table(
            String title, String caption, List<TableColumn> columns, List<TableRow> rows
    ) implements ContentBlock, LayoutChildBlock {
        public Table(String caption, List<TableColumn> columns, List<TableRow> rows) {
            this(null, caption, columns, rows);
        }
    }

    /**
     * Multi-column section grouping existing IMAGE/TABLE blocks (see {@link LayoutChildBlock}).
     * {@code columns} is the requested column count on desktop; the renderer collapses to fewer
     * columns on narrower viewports (see docs/CMS_LAYOUT_RENDERING_CONTRACT.md) — never a raw
     * CSS/pixel value. {@code children} auto-wraps into additional rows once it exceeds
     * {@code columns} (CSS Grid semantics) — there is no separate "row" concept to keep in sync.
     * No {@code gap}/spacing field: the renderer owns one consistent spacing value, keeping the
     * V1 surface area minimal (additive to add later if editors need control over it).
     */
    @JsonTypeName("LAYOUT")
    record Layout(int columns, List<LayoutChildBlock> children) implements ContentBlock {
    }

    record TableColumn(String label) {
        @JsonAnySetter
        public void rejectUnknownProperty(String property, Object ignored) {
            throw new IllegalArgumentException("Unknown table-column property: " + property);
        }
    }

    record TableRow(TableRowType rowType, List<List<InlineNode>> cells) {
        @JsonAnySetter
        public void rejectUnknownProperty(String property, Object ignored) {
            throw new IllegalArgumentException("Unknown table-row property: " + property);
        }
    }
}
