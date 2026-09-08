package com.brandPitara.sfs.cms.content.document;

import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ContentDocumentValidator {

    private static final Pattern YOUTUBE_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private final ObjectMapper objectMapper;

    public ContentDocument validateAndNormalize(ContentDocument document) {
        if (document == null) {
            throw CmsContentApiException.documentInvalid("Document is required.");
        }
        if (document.schemaVersion() < ContentDocument.MINIMUM_READABLE_SCHEMA_VERSION
                || document.schemaVersion() > ContentDocument.CURRENT_SCHEMA_VERSION) {
            throw CmsContentApiException.documentSchemaUnsupported(document.schemaVersion());
        }
        if (document.blocks() == null) {
            throw CmsContentApiException.documentInvalid("Document blocks are required.");
        }
        if (document.blocks().size() > ContentDocumentLimits.MAX_BLOCKS) {
            throw CmsContentApiException.documentInvalid(
                    "Document exceeds the maximum of " + ContentDocumentLimits.MAX_BLOCKS + " blocks."
            );
        }

        Counters counters = new Counters();
        List<ContentBlock> normalizedBlocks = new ArrayList<>(document.blocks().size());
        for (ContentBlock block : document.blocks()) {
            normalizedBlocks.add(normalizeBlock(block, document.schemaVersion(), counters));
        }
        ContentDocument normalized = new ContentDocument(
                ContentDocument.CURRENT_SCHEMA_VERSION,
                List.copyOf(normalizedBlocks)
        );
        enforceSerializedSize(normalized);
        return normalized;
    }

    private ContentBlock normalizeBlock(ContentBlock block, int sourceSchemaVersion, Counters counters) {
        if (block == null) {
            throw CmsContentApiException.documentInvalid("Document blocks cannot be null.");
        }
        if (block instanceof ContentBlock.Paragraph paragraph) {
            return new ContentBlock.Paragraph(normalizeInlineContent(
                    paragraph.content(), ContentDocumentLimits.MAX_INLINE_NODES_PER_BLOCK,
                    false, "Paragraph", counters
            ));
        }
        if (block instanceof ContentBlock.Heading heading) {
            if (heading.level() == null) {
                throw CmsContentApiException.documentInvalid("Heading level must be H2, H3, or H4.");
            }
            return new ContentBlock.Heading(heading.level(), normalizeInlineContent(
                    heading.content(), ContentDocumentLimits.MAX_INLINE_NODES_PER_BLOCK,
                    true, "Heading", counters
            ));
        }
        if (block instanceof ContentBlock.Blockquote quote) {
            return new ContentBlock.Blockquote(normalizeInlineContent(
                    quote.content(), ContentDocumentLimits.MAX_INLINE_NODES_PER_BLOCK,
                    true, "Blockquote", counters
            ));
        }
        if (block instanceof ContentBlock.BulletList list) {
            return new ContentBlock.BulletList(normalizeListItems(list.items(), "Bullet list", counters));
        }
        if (block instanceof ContentBlock.OrderedList list) {
            return new ContentBlock.OrderedList(normalizeListItems(list.items(), "Ordered list", counters));
        }
        if (block instanceof ContentBlock.Divider) {
            return new ContentBlock.Divider();
        }
        if (sourceSchemaVersion < 2) {
            throw CmsContentApiException.documentSchemaUnsupported(sourceSchemaVersion);
        }
        if (block instanceof ContentBlock.Image image) {
            return normalizeImage(image, counters);
        }
        if (block instanceof ContentBlock.Video video) {
            return new ContentBlock.Video(
                    requirePositiveId(video.mediaAssetId(), "Video media asset"),
                    video.posterMediaAssetId() == null
                            ? null : requirePositiveId(video.posterMediaAssetId(), "Video poster media asset"),
                    normalizeCaption(video.caption(), "Video caption", counters)
            );
        }
        if (block instanceof ContentBlock.Embed embed) {
            if (embed.provider() != EmbedProvider.YOUTUBE) {
                throw CmsContentApiException.documentInvalid("Only YOUTUBE embeds are supported.");
            }
            return new ContentBlock.Embed(
                    EmbedProvider.YOUTUBE,
                    normalizeYouTubeExternalId(embed.externalId()),
                    normalizeCaption(embed.caption(), "Embed caption", counters)
            );
        }
        if (sourceSchemaVersion < 3) {
            throw CmsContentApiException.documentSchemaUnsupported(sourceSchemaVersion);
        }
        if (block instanceof ContentBlock.CheckList checkList) {
            return new ContentBlock.CheckList(normalizeListItems(checkList.items(), "Check list", counters));
        }
        if (block instanceof ContentBlock.Callout callout) {
            return normalizeCallout(callout, counters);
        }
        if (block instanceof ContentBlock.Table table) {
            return normalizeTable(table, counters);
        }
        if (sourceSchemaVersion < 4) {
            throw CmsContentApiException.documentSchemaUnsupported(sourceSchemaVersion);
        }
        if (block instanceof ContentBlock.Layout layout) {
            return normalizeLayout(layout, counters);
        }
        throw CmsContentApiException.documentInvalid("Unsupported content block.");
    }

    private ContentBlock.Layout normalizeLayout(ContentBlock.Layout layout, Counters counters) {
        if (layout.columns() < ContentDocumentLimits.MIN_LAYOUT_COLUMNS
                || layout.columns() > ContentDocumentLimits.MAX_LAYOUT_COLUMNS) {
            throw CmsContentApiException.documentInvalid(
                    "Layout columns must be between " + ContentDocumentLimits.MIN_LAYOUT_COLUMNS
                            + " and " + ContentDocumentLimits.MAX_LAYOUT_COLUMNS + "."
            );
        }
        if (layout.children() == null || layout.children().isEmpty()) {
            throw CmsContentApiException.documentInvalid("Layout must contain at least one child block.");
        }
        if (layout.children().size() > ContentDocumentLimits.MAX_LAYOUT_CHILDREN) {
            throw CmsContentApiException.documentInvalid(
                    "Layout exceeds the maximum of " + ContentDocumentLimits.MAX_LAYOUT_CHILDREN + " children."
            );
        }
        List<LayoutChildBlock> children = new ArrayList<>(layout.children().size());
        for (LayoutChildBlock child : layout.children()) {
            if (child == null) {
                throw CmsContentApiException.documentInvalid("Layout children cannot be null.");
            }
            // Each child is normalized through the SAME per-type logic a top-level IMAGE/TABLE
            // uses (normalizeImage/normalizeTable) - a layout child is validated exactly as
            // strictly as a standalone one, never a separate/looser path.
            if (child instanceof ContentBlock.Image image) {
                children.add(normalizeImage(image, counters));
            } else if (child instanceof ContentBlock.Table table) {
                children.add(normalizeTable(table, counters));
            } else {
                throw CmsContentApiException.documentInvalid("Unsupported layout child block.");
            }
        }
        return new ContentBlock.Layout(layout.columns(), List.copyOf(children));
    }

    private ContentBlock.Callout normalizeCallout(ContentBlock.Callout callout, Counters counters) {
        if (callout.variant() == null) {
            throw CmsContentApiException.documentInvalid("Callout variant is required.");
        }
        String title = trackText(normalizeBoundedLabel(
                callout.title(), ContentDocumentLimits.MAX_CALLOUT_TITLE_CHARACTERS, "Callout title"
        ), counters);
        List<InlineNode> content = normalizeInlineContent(
                callout.content(), ContentDocumentLimits.MAX_INLINE_NODES_PER_BLOCK,
                true, "Callout", counters
        );
        return new ContentBlock.Callout(callout.variant(), title, content);
    }

    private ContentBlock.Table normalizeTable(ContentBlock.Table table, Counters counters) {
        String title = trackText(normalizeBoundedLabel(
                table.title(), ContentDocumentLimits.MAX_TABLE_TITLE_CHARACTERS, "Table title"
        ), counters);
        String caption = trackText(normalizeBoundedLabel(
                table.caption(), ContentDocumentLimits.MAX_TABLE_CAPTION_CHARACTERS, "Table caption"
        ), counters);
        if (table.columns() == null || table.columns().isEmpty()) {
            throw CmsContentApiException.documentInvalid("Table must have at least one column.");
        }
        if (table.columns().size() > ContentDocumentLimits.MAX_TABLE_COLUMNS) {
            throw CmsContentApiException.documentInvalid(
                    "Table exceeds the maximum of " + ContentDocumentLimits.MAX_TABLE_COLUMNS + " columns."
            );
        }
        List<ContentBlock.TableColumn> columns = new ArrayList<>(table.columns().size());
        for (ContentBlock.TableColumn column : table.columns()) {
            if (column == null) {
                throw CmsContentApiException.documentInvalid("Table columns cannot be null.");
            }
            String label = trackText(normalizeBoundedLabel(
                    column.label(), ContentDocumentLimits.MAX_TABLE_COLUMN_LABEL_CHARACTERS, "Table column label"
            ), counters);
            if (label == null) {
                throw CmsContentApiException.documentInvalid("Table column label is required.");
            }
            columns.add(new ContentBlock.TableColumn(label));
        }

        if (table.rows() == null || table.rows().isEmpty()) {
            throw CmsContentApiException.documentInvalid("Table must have at least one row.");
        }
        if (table.rows().size() > ContentDocumentLimits.MAX_TABLE_ROWS) {
            throw CmsContentApiException.documentInvalid(
                    "Table exceeds the maximum of " + ContentDocumentLimits.MAX_TABLE_ROWS + " rows."
            );
        }
        boolean anyVisibleCell = false;
        List<ContentBlock.TableRow> rows = new ArrayList<>(table.rows().size());
        for (ContentBlock.TableRow row : table.rows()) {
            if (row == null || row.rowType() == null) {
                throw CmsContentApiException.documentInvalid("Table rows require a row type.");
            }
            int expectedCells = row.rowType() == TableRowType.SECTION ? 1 : columns.size();
            if (row.cells() == null || row.cells().size() != expectedCells) {
                throw CmsContentApiException.documentInvalid(
                        row.rowType() == TableRowType.SECTION
                                ? "A SECTION row must have exactly one full-width cell."
                                : "Table row must have exactly " + columns.size() + " cells to match its columns."
                );
            }
            List<List<InlineNode>> cells = new ArrayList<>(row.cells().size());
            for (List<InlineNode> cell : row.cells()) {
                List<InlineNode> normalizedCell = normalizeInlineContent(
                        cell, ContentDocumentLimits.MAX_TABLE_CELL_INLINE_NODES,
                        false, "Table cell", counters
                );
                anyVisibleCell |= normalizedCell.stream()
                        .filter(InlineNode.Text.class::isInstance)
                        .map(InlineNode.Text.class::cast)
                        .anyMatch(text -> !text.text().isBlank());
                cells.add(normalizedCell);
            }
            rows.add(new ContentBlock.TableRow(row.rowType(), List.copyOf(cells)));
        }
        if (!anyVisibleCell) {
            throw CmsContentApiException.documentInvalid("Table must contain at least one cell with visible text.");
        }
        return new ContentBlock.Table(title, caption, List.copyOf(columns), List.copyOf(rows));
    }

    /**
     * Counts a short presentation label (table caption/column label, callout title) toward
     * the document-wide text budget, same as every other text source (Step 14: "all limits
     * must count toward existing overall document limits").
     */
    private String trackText(String value, Counters counters) {
        if (value == null) {
            return null;
        }
        counters.textCharacters += value.length();
        if (counters.textCharacters > ContentDocumentLimits.MAX_TOTAL_TEXT_CHARACTERS) {
            throw CmsContentApiException.documentInvalid(
                    "Document exceeds " + ContentDocumentLimits.MAX_TOTAL_TEXT_CHARACTERS
                            + " total text characters."
            );
        }
        return value;
    }

    /**
     * Shared normalization for short plain-text presentation labels (table captions/column
     * labels, callout titles) — same bounded-length, control-character-free treatment already
     * applied to {@link ContentBlock.Image#altText()}. Returns null for blank/absent input so
     * these labels stay genuinely optional rather than becoming empty strings.
     */
    private String normalizeBoundedLabel(String value, int maxCharacters, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxCharacters || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw CmsContentApiException.documentInvalid(
                    label + " must be at most " + maxCharacters + " characters without control characters."
            );
        }
        return normalized;
    }

    private ContentBlock.Image normalizeImage(ContentBlock.Image image, Counters counters) {
        Long mediaAssetId = requirePositiveId(image.mediaAssetId(), "Image media asset");
        String altText = image.altText() == null ? null : image.altText().trim();
        if (image.decorative()) {
            if (StringUtils.hasText(altText)) {
                throw CmsContentApiException.documentInvalid(
                        "Decorative images must not have alt text."
                );
            }
            altText = null;
        } else if (!StringUtils.hasText(altText)) {
            throw CmsContentApiException.documentInvalid(
                    "Non-decorative images require meaningful alt text."
            );
        }
        if (altText != null) {
            if (altText.length() > ContentDocumentLimits.MAX_ALT_TEXT_CHARACTERS
                    || altText.codePoints().anyMatch(Character::isISOControl)) {
                throw CmsContentApiException.documentInvalid(
                        "Image alt text must be at most "
                                + ContentDocumentLimits.MAX_ALT_TEXT_CHARACTERS
                                + " characters without control characters."
                );
            }
        }
        ContentLink link = image.link() == null ? null : new ContentLink(
                normalizeLink(image.link().href()),
                image.link().openInNewTab(),
                image.link().nofollow(),
                image.link().sponsored()
        );
        return new ContentBlock.Image(
                mediaAssetId,
                image.decorative(),
                altText,
                normalizeCaption(image.caption(), "Image caption", counters),
                image.layout() == null ? ImageLayout.STANDARD : image.layout(),
                link
        );
    }

    private Long requirePositiveId(Long id, String label) {
        if (id == null || id <= 0) {
            throw CmsContentApiException.documentInvalid(label + " ID must be a positive number.");
        }
        return id;
    }

    private List<InlineNode> normalizeCaption(
            List<InlineNode> caption,
            String label,
            Counters counters
    ) {
        if (caption == null) {
            return List.of();
        }
        int before = counters.textCharacters;
        List<InlineNode> normalized = normalizeInlineContent(
                caption,
                ContentDocumentLimits.MAX_CAPTION_INLINE_NODES,
                false,
                label,
                counters
        );
        if (counters.textCharacters - before > ContentDocumentLimits.MAX_CAPTION_CHARACTERS) {
            throw CmsContentApiException.documentInvalid(
                    label + " exceeds " + ContentDocumentLimits.MAX_CAPTION_CHARACTERS + " characters."
            );
        }
        return normalized;
    }

    private String normalizeYouTubeExternalId(String value) {
        if (!StringUtils.hasText(value)) {
            throw CmsContentApiException.documentInvalid("YouTube externalId is required.");
        }
        String candidate = value.trim();
        if (YOUTUBE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        try {
            URI uri = new URI(candidate);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw invalidYouTube();
            }
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String id = null;
            if (Set.of("youtube.com", "www.youtube.com", "m.youtube.com").contains(host)) {
                if ("/watch".equals(uri.getPath())) {
                    id = queryParameter(uri.getRawQuery(), "v");
                } else if (uri.getPath() != null && uri.getPath().startsWith("/shorts/")) {
                    id = firstPathSegment(uri.getPath().substring("/shorts/".length()));
                }
            } else if (Set.of("youtu.be", "www.youtu.be").contains(host)) {
                id = firstPathSegment(uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", ""));
            }
            if (id == null || !YOUTUBE_ID.matcher(id).matches()) {
                throw invalidYouTube();
            }
            return id;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            if (exception instanceof CmsContentApiException contentException) {
                throw contentException;
            }
            throw invalidYouTube();
        }
    }

    private String queryParameter(String rawQuery, String name) {
        if (rawQuery == null) return null;
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && name.equals(URLDecoder.decode(parts[0], StandardCharsets.UTF_8))) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String firstPathSegment(String path) {
        if (!StringUtils.hasText(path)) return null;
        int separator = path.indexOf('/');
        return separator < 0 ? path : path.substring(0, separator);
    }

    private CmsContentApiException invalidYouTube() {
        return CmsContentApiException.documentInvalid(
                "YouTube embed must use a canonical video ID or an approved HTTPS YouTube URL."
        );
    }

    private List<ContentBlock.ListItem> normalizeListItems(
            List<ContentBlock.ListItem> items,
            String label,
            Counters counters
    ) {
        if (items == null || items.isEmpty()) {
            throw CmsContentApiException.documentInvalid(label + " must contain at least one item.");
        }
        if (items.size() > ContentDocumentLimits.MAX_LIST_ITEMS) {
            throw CmsContentApiException.documentInvalid(
                    label + " exceeds " + ContentDocumentLimits.MAX_LIST_ITEMS + " items."
            );
        }
        List<ContentBlock.ListItem> normalized = new ArrayList<>(items.size());
        for (ContentBlock.ListItem item : items) {
            if (item == null) {
                throw CmsContentApiException.documentInvalid(label + " items cannot be null.");
            }
            normalized.add(new ContentBlock.ListItem(normalizeInlineContent(
                    item.content(), ContentDocumentLimits.MAX_INLINE_NODES_PER_LIST_ITEM,
                    true, "List item", counters
            )));
        }
        return List.copyOf(normalized);
    }

    private List<InlineNode> normalizeInlineContent(
            List<InlineNode> content,
            int maximumNodes,
            boolean requireText,
            String label,
            Counters counters
    ) {
        if (content == null) {
            throw CmsContentApiException.documentInvalid(label + " content is required.");
        }
        if (content.size() > maximumNodes) {
            throw CmsContentApiException.documentInvalid(
                    label + " exceeds the maximum inline-node count of " + maximumNodes + "."
            );
        }
        List<InlineNode> normalized = new ArrayList<>(content.size());
        boolean hasVisibleText = false;
        for (InlineNode node : content) {
            if (node instanceof InlineNode.Text textNode) {
                String text = normalizeLineEndings(textNode.text());
                if (text.isEmpty()) {
                    throw CmsContentApiException.documentInvalid("Text nodes cannot be empty.");
                }
                if (text.length() > ContentDocumentLimits.MAX_TEXT_NODE_CHARACTERS) {
                    throw CmsContentApiException.documentInvalid(
                            "Text node exceeds " + ContentDocumentLimits.MAX_TEXT_NODE_CHARACTERS + " characters."
                    );
                }
                counters.textCharacters += text.length();
                if (counters.textCharacters > ContentDocumentLimits.MAX_TOTAL_TEXT_CHARACTERS) {
                    throw CmsContentApiException.documentInvalid(
                            "Document exceeds " + ContentDocumentLimits.MAX_TOTAL_TEXT_CHARACTERS
                                    + " total text characters."
                    );
                }
                hasVisibleText |= !text.isBlank();
                normalized.add(new InlineNode.Text(text, normalizeMarks(textNode.marks())));
            } else if (node instanceof InlineNode.HardBreak) {
                normalized.add(new InlineNode.HardBreak());
            } else {
                throw CmsContentApiException.documentInvalid("Unsupported or null inline node.");
            }
        }
        if (requireText && !hasVisibleText) {
            throw CmsContentApiException.documentInvalid(label + " must contain visible text.");
        }
        return List.copyOf(normalized);
    }

    private List<TextMark> normalizeMarks(List<TextMark> marks) {
        if (marks == null || marks.isEmpty()) {
            return List.of();
        }
        if (marks.size() > ContentDocumentLimits.MAX_MARKS_PER_TEXT_NODE) {
            throw CmsContentApiException.documentInvalid(
                    "Text node exceeds " + ContentDocumentLimits.MAX_MARKS_PER_TEXT_NODE + " marks."
            );
        }
        Set<Class<?>> seen = new HashSet<>();
        List<TextMark> normalized = new ArrayList<>(marks.size());
        for (TextMark mark : marks) {
            if (mark == null || !seen.add(mark.getClass())) {
                throw CmsContentApiException.documentInvalid("Text marks must be known and unique by type.");
            }
            if (mark instanceof TextMark.Bold) {
                normalized.add(new TextMark.Bold());
            } else if (mark instanceof TextMark.Italic) {
                normalized.add(new TextMark.Italic());
            } else if (mark instanceof TextMark.Underline) {
                normalized.add(new TextMark.Underline());
            } else if (mark instanceof TextMark.Link link) {
                normalized.add(new TextMark.Link(
                        normalizeLink(link.href()),
                        link.openInNewTab(),
                        link.nofollow(),
                        link.sponsored()
                ));
            } else {
                throw CmsContentApiException.documentInvalid("Unsupported text mark.");
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeLink(String value) {
        if (!StringUtils.hasText(value)) {
            throw CmsContentApiException.documentInvalid("Link href is required.");
        }
        String href = value.trim();
        if (href.codePoints().anyMatch(Character::isISOControl) || href.contains("\\")) {
            throw CmsContentApiException.documentInvalid("Link href contains unsafe characters.");
        }
        try {
            URI uri = new URI(href);
            if (uri.isAbsolute()) {
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || !StringUtils.hasText(uri.getHost())
                        || uri.getUserInfo() != null) {
                    throw CmsContentApiException.documentInvalid(
                            "Links must use HTTPS without embedded credentials."
                    );
                }
                return uri.normalize().toASCIIString();
            }
            if (!href.startsWith("/") || href.startsWith("//") || uri.getRawAuthority() != null) {
                throw CmsContentApiException.documentInvalid(
                        "Relative links must be root-relative SFS paths."
                );
            }
            return uri.normalize().toASCIIString();
        } catch (URISyntaxException exception) {
            throw CmsContentApiException.documentInvalid("Link href is not a valid URI.");
        }
    }

    private String normalizeLineEndings(String text) {
        if (text == null) {
            throw CmsContentApiException.documentInvalid("Text is required.");
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private void enforceSerializedSize(ContentDocument document) {
        try {
            int bytes = objectMapper.writeValueAsString(document).getBytes(StandardCharsets.UTF_8).length;
            if (bytes > ContentDocumentLimits.MAX_SERIALIZED_BYTES) {
                throw CmsContentApiException.documentTooLarge(ContentDocumentLimits.MAX_SERIALIZED_BYTES);
            }
        } catch (JsonProcessingException exception) {
            throw CmsContentApiException.documentInvalid("Document could not be serialized safely.");
        }
    }

    private static final class Counters {
        private int textCharacters;
    }
}
