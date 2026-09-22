package com.brandPitara.sfs.migration.wordpress.gutenberg;

import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.ContentDocumentLimits;
import com.brandPitara.sfs.cms.content.document.ContentDocumentValidator;
import com.brandPitara.sfs.cms.content.document.EmbedProvider;
import com.brandPitara.sfs.cms.content.document.HeadingLevel;
import com.brandPitara.sfs.cms.content.document.ImageLayout;
import com.brandPitara.sfs.cms.content.document.InlineNode;
import com.brandPitara.sfs.cms.content.document.TextMark;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts one WordPress post's Gutenberg {@code post_content} into a schema-v5
 * {@link ContentDocument}, per the block mapping documented on
 * {@link com.brandPitara.sfs.migration.wordpress.gutenberg}. Media blocks reference the
 * WordPress attachment ID directly as a placeholder {@code mediaAssetId} - real resolution to a
 * {@code CmsMediaAssetEntity} ID happens in a later step; this converter's job is structural,
 * not upload orchestration. The assembled document is always run through the real
 * {@link ContentDocumentValidator} before being returned, so "does this pass CMS validation" is
 * never re-implemented here - a validator rejection becomes a blocking error.
 */
public final class GutenbergDocumentConverter {

    private static final Pattern WP_IMAGE_CLASS_ID = Pattern.compile("wp-image-(\\d+)");

    private final GutenbergBlockParser blockParser;
    private final ContentDocumentValidator validator;

    public GutenbergDocumentConverter() {
        ObjectMapper mapper = new ObjectMapper();
        this.blockParser = new GutenbergBlockParser(mapper);
        this.validator = new ContentDocumentValidator(mapper);
    }

    public GutenbergConversionResult convert(long wordPressPostId, String title, String postContent) {
        ConversionContext ctx = new ConversionContext();
        List<GutenbergBlock> topLevel = blockParser.parse(postContent == null ? "" : postContent);
        List<ContentBlock> converted = new ArrayList<>();
        for (int i = 0; i < topLevel.size(); i++) {
            converted.addAll(convertBlock(topLevel.get(i), "[" + i + "]", ctx));
        }

        ContentDocument document = new ContentDocument(5, List.copyOf(converted));
        boolean validatorRejected = false;
        try {
            // Use the validator's own normalized output (not just a pass/fail check) so every
            // canonicalization it applies - YouTube URL -> bare video ID, default image layout,
            // trimmed labels - actually lands in the returned document, not just gets checked.
            document = validator.validateAndNormalize(document);
        } catch (CmsContentApiException rejection) {
            validatorRejected = true;
            ctx.blockingErrors.add(new ConversionBlockingError(
                    "DOCUMENT", "/", "Converted document failed CMS validation: " + rejection.getMessage()
            ));
        }

        boolean manualReview = !ctx.blockingErrors.isEmpty()
                || ctx.unsupportedBlocks.stream().anyMatch(b -> b.disposition() == UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED);
        boolean eligible = !manualReview && !validatorRejected;

        return new GutenbergConversionResult(
                wordPressPostId, title, document,
                List.copyOf(ctx.warnings), List.copyOf(ctx.blockingErrors), List.copyOf(ctx.unsupportedBlocks),
                List.copyOf(ctx.unresolvedGalleries),
                Set.copyOf(ctx.referencedAttachmentIds), Set.copyOf(ctx.referencedExternalUrls),
                manualReview, eligible
        );
    }

    // ── Per-block dispatch ─────────────────────────────────────────────────────

    private List<ContentBlock> convertBlock(GutenbergBlock block, String path, ConversionContext ctx) {
        if (block.isFreeform()) {
            return convertFreeform(block, path, ctx);
        }
        String name = block.normalizedBlockName();
        return switch (name) {
            case "core/paragraph" -> single(convertParagraph(block, path, ctx));
            case "core/heading" -> single(convertHeading(block, path, ctx));
            case "core/list" -> single(convertList(block, path, ctx));
            case "core/list-item" -> List.of(); // only ever reached if malformed/unnested - see convertList
            case "core/quote", "core/pullquote" -> single(convertQuote(block, path, ctx));
            case "core/separator" -> single(new ContentBlock.Divider());
            case "core/image" -> optionalToList(convertImage(block, path, ctx));
            case "core/gallery" -> optionalToList(convertGallery(block, path, ctx));
            case "core/video" -> optionalToList(convertVideo(block, path, ctx));
            case "core/embed", "core-embed/youtube" -> convertEmbedBlock(block, path, ctx);
            case "core/table" -> optionalToList(convertTable(block, path, ctx));
            case "core/columns", "core/group" -> convertColumnsOrGroup(block, path, ctx, name);
            case "core/column" -> convertChildrenFlattened(block, path, ctx);
            case "core/media-text" -> convertMediaText(block, path, ctx);
            case "core/spacer" -> {
                ctx.warnings.add(new ConversionWarning(name, path, "Spacer omitted (no meaningful structure)."));
                yield List.of();
            }
            case "feedbackwp/rating-widget" -> {
                ctx.warnings.add(new ConversionWarning(name, path,
                        "Rating widget omitted - belongs to the website template, not the article body."));
                yield List.of();
            }
            case "qligg/box" -> {
                boolean meaningful = !InlineHtmlExtractor.plainText(block.innerHtml()).isBlank();
                ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                        name, path, meaningful, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
                ));
                yield List.of();
            }
            case "core/html" -> convertRawHtml(block, path, ctx);
            default -> convertUnknown(block, path, ctx, name);
        };
    }

    private List<ContentBlock> single(ContentBlock block) {
        return block == null ? List.of() : List.of(block);
    }

    private List<ContentBlock> optionalToList(ContentBlock block) {
        return block == null ? List.of() : List.of(block);
    }

    // ── Leaf blocks ────────────────────────────────────────────────────────────

    private ContentBlock convertParagraph(GutenbergBlock block, String path, ConversionContext ctx) {
        List<InlineNode> content = extractSafe(block.innerHtml(), "core/paragraph", path, ctx);
        if (content.isEmpty()) {
            return null;
        }
        return new ContentBlock.Paragraph(content);
    }

    private ContentBlock convertHeading(GutenbergBlock block, String path, ConversionContext ctx) {
        int wpLevel = block.attributes().path("level").asInt(2);
        HeadingLevel level;
        if (wpLevel <= 2) {
            level = HeadingLevel.H2;
            if (wpLevel == 1) {
                ctx.warnings.add(new ConversionWarning("core/heading", path, "H1 downgraded to H2 (CMS supports H2-H4 only)."));
            }
        } else if (wpLevel == 3) {
            level = HeadingLevel.H3;
        } else {
            level = HeadingLevel.H4;
            if (wpLevel > 4) {
                ctx.warnings.add(new ConversionWarning("core/heading", path, "H" + wpLevel + " downgraded to H4 (CMS supports H2-H4 only)."));
            }
        }
        List<InlineNode> content = extractSafe(block.innerHtml(), "core/heading", path, ctx);
        if (content.isEmpty()) {
            return null;
        }
        return new ContentBlock.Heading(level, content);
    }

    private ContentBlock convertList(GutenbergBlock block, String path, ConversionContext ctx) {
        boolean ordered = block.attributes().path("ordered").asBoolean(false);
        List<ContentBlock.ListItem> items = new ArrayList<>();
        String blockName = ordered ? "core/list (ordered)" : "core/list";
        if (!block.children().isEmpty()) {
            for (GutenbergBlock child : block.children()) {
                if ("core/list-item".equals(child.normalizedBlockName())) {
                    List<InlineNode> content = extractSafe(child.innerHtml(), blockName, path, ctx);
                    if (!content.isEmpty()) {
                        items.add(new ContentBlock.ListItem(content));
                    }
                }
            }
        } else {
            for (Element li : Jsoup.parseBodyFragment(block.innerHtml(), "").body().select("li")) {
                List<InlineNode> content = extractSafe(li.html(), blockName, path, ctx);
                if (!content.isEmpty()) {
                    items.add(new ContentBlock.ListItem(content));
                }
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        return ordered ? new ContentBlock.OrderedList(items) : new ContentBlock.BulletList(items);
    }

    private ContentBlock convertQuote(GutenbergBlock block, String path, ConversionContext ctx) {
        Element root = Jsoup.parseBodyFragment(block.innerHtml(), "").body();
        Element cite = root.selectFirst("cite");
        String citation = cite == null ? null : cite.text();
        if (cite != null) {
            cite.remove();
        }
        List<InlineNode> content = extractSafe(root.html(), "core/quote", path, ctx);
        if (content.isEmpty()) {
            return null;
        }
        if (citation != null && !citation.isBlank()) {
            ctx.warnings.add(new ConversionWarning(
                    block.normalizedBlockName(), path,
                    "Quote attribution \"" + citation + "\" dropped - BLOCKQUOTE has no citation field."
            ));
        }
        return new ContentBlock.Blockquote(content);
    }

    // ── Media blocks ───────────────────────────────────────────────────────────

    private ContentBlock convertImage(GutenbergBlock block, String path, ConversionContext ctx) {
        Element root = Jsoup.parseBodyFragment(block.innerHtml(), "").body();
        Element img = root.selectFirst("img");
        Long attachmentId = attachmentIdOf(block.attributes(), img);
        String altText = sanitizeAltText(
                block.attributes().path("alt").asText(img == null ? "" : img.attr("alt")), "core/image", path, ctx
        );
        boolean decorative = altText == null || altText.isBlank();
        Element figcaption = root.selectFirst("figcaption");
        List<InlineNode> caption = figcaption == null ? List.of()
                : extractSafe(figcaption.html(), "core/image", path, ctx);

        if (attachmentId == null) {
            String url = img == null ? null : img.attr("src");
            if (url != null && !url.isBlank()) {
                ctx.referencedExternalUrls.add(url);
            }
            ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                    "core/image", path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
            ));
            return null;
        }
        ctx.referencedAttachmentIds.add(attachmentId);
        String layoutClass = root.className();
        ImageLayout layout = layoutClass.contains("alignwide") || layoutClass.contains("alignfull")
                ? ImageLayout.WIDE : ImageLayout.STANDARD;
        return new ContentBlock.Image(attachmentId, decorative, decorative ? null : altText, caption, layout, null);
    }

    private ContentBlock convertGallery(GutenbergBlock block, String path, ConversionContext ctx) {
        List<ContentBlock.GalleryImage> images = new ArrayList<>();
        List<Long> resolvedIds = new ArrayList<>();
        List<String> unresolvedUrls = new ArrayList<>();

        List<GutenbergBlock> imageChildren = block.children().stream()
                .filter(c -> "core/image".equals(c.normalizedBlockName()))
                .toList();
        if (!imageChildren.isEmpty()) {
            for (int i = 0; i < imageChildren.size(); i++) {
                GutenbergBlock child = imageChildren.get(i);
                Element root = Jsoup.parseBodyFragment(child.innerHtml(), "").body();
                Element img = root.selectFirst("img");
                Long attachmentId = attachmentIdOf(child.attributes(), img);
                if (attachmentId == null) {
                    String url = img == null ? null : img.attr("src");
                    if (url != null && !url.isBlank()) {
                        unresolvedUrls.add(url);
                        ctx.referencedExternalUrls.add(url);
                    }
                    continue;
                }
                ctx.referencedAttachmentIds.add(attachmentId);
                resolvedIds.add(attachmentId);
                String alt = sanitizeAltText(
                        child.attributes().path("alt").asText(img == null ? "" : img.attr("alt")), "core/gallery", path, ctx
                );
                boolean decorative = alt == null || alt.isBlank();
                images.add(new ContentBlock.GalleryImage(attachmentId, decorative, decorative ? null : alt, List.of()));
            }
        } else {
            Element root = Jsoup.parseBodyFragment(block.innerHtml(), "").body();
            for (Element img : root.select("img")) {
                Long attachmentId = attachmentIdFromClass(img.className());
                if (attachmentId == null) {
                    String url = img.attr("src");
                    if (!url.isBlank()) {
                        unresolvedUrls.add(url);
                        ctx.referencedExternalUrls.add(url);
                    }
                    continue;
                }
                ctx.referencedAttachmentIds.add(attachmentId);
                resolvedIds.add(attachmentId);
                String alt = sanitizeAltText(img.attr("alt"), "core/gallery", path, ctx);
                boolean decorative = alt == null || alt.isBlank();
                images.add(new ContentBlock.GalleryImage(attachmentId, decorative, decorative ? null : alt, List.of()));
            }
        }

        if (!unresolvedUrls.isEmpty()) {
            // A structured, repairable record - never a fabricated attachment ID and never the
            // raw URL stored as a final CMS media reference (see UnresolvedGalleryReport).
            ctx.unresolvedGalleries.add(new UnresolvedGalleryReport(path, List.copyOf(resolvedIds), List.copyOf(unresolvedUrls)));
        }
        if (images.isEmpty()) {
            ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                    "core/gallery", path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
            ));
            return null;
        }
        if (!unresolvedUrls.isEmpty()) {
            ctx.warnings.add(new ConversionWarning("core/gallery", path,
                    unresolvedUrls.size() + " of " + (images.size() + unresolvedUrls.size())
                            + " gallery image(s) had no resolvable attachment ID and were dropped from the gallery."));
        }
        Integer columns = block.attributes().has("columns") ? block.attributes().get("columns").asInt() : null;
        return new ContentBlock.Gallery(columns, images);
    }

    private ContentBlock convertVideo(GutenbergBlock block, String path, ConversionContext ctx) {
        Element root = Jsoup.parseBodyFragment(block.innerHtml(), "").body();
        Element video = root.selectFirst("video");
        Long attachmentId = attachmentIdOf(block.attributes(), null);
        if (attachmentId == null && video != null) {
            attachmentId = attachmentIdFromClass(video.className());
        }
        if (attachmentId == null) {
            String url = video == null ? null : video.attr("src");
            if (url != null && !url.isBlank()) {
                ctx.referencedExternalUrls.add(url);
            }
            ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                    "core/video", path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
            ));
            return null;
        }
        ctx.referencedAttachmentIds.add(attachmentId);
        Element figcaption = root.selectFirst("figcaption");
        List<InlineNode> caption = figcaption == null ? List.of() : extractSafe(figcaption.html(), "core/video", path, ctx);
        return new ContentBlock.Video(attachmentId, null, caption);
    }

    /**
     * Unlike other converters, this returns a {@code List} directly (not via {@link #optionalToList}):
     * a YouTube URL in a format {@link #extractYouTubeId} cannot unambiguously resolve must still
     * preserve its visible/safe URL as ordinary content (a plain paragraph) rather than either
     * inventing a wrong ID or silently dropping the reference - see Section 2 remediation rules.
     */
    private List<ContentBlock> convertEmbedBlock(GutenbergBlock block, String path, ConversionContext ctx) {
        String provider = block.attributes().path("providerNameSlug").asText("");
        String url = block.attributes().path("url").asText("");
        if (url.isBlank()) {
            Element root = Jsoup.parseBodyFragment(block.innerHtml(), "").body();
            Element anchor = root.selectFirst("a[href]");
            if (anchor != null) {
                url = anchor.attr("href");
            }
        }
        boolean looksLikeYouTube = provider.contains("youtube") || url.contains("youtube.com") || url.contains("youtu.be");
        if (!looksLikeYouTube || url.isBlank()) {
            ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                    block.normalizedBlockName(), path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
            ));
            return List.of();
        }
        java.util.Optional<String> id = extractYouTubeId(url);
        if (id.isPresent()) {
            return List.of(new ContentBlock.Embed(EmbedProvider.YOUTUBE, id.get(), List.of()));
        }
        ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                "core/embed", path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
        ));
        ctx.warnings.add(new ConversionWarning("core/embed", path,
                "YouTube URL format not recognized (" + url + "); preserved as plain text instead of an EMBED block."));
        return List.of(new ContentBlock.Paragraph(List.of(new InlineNode.Text(url, List.of()))));
    }

    private static final Pattern YOUTUBE_ID_SHAPE = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    /** Recognizes watch/shorts/live/embed/v/youtu.be URL shapes and bare 11-char IDs; empty otherwise. */
    private java.util.Optional<String> extractYouTubeId(String rawUrl) {
        if (rawUrl == null) {
            return java.util.Optional.empty();
        }
        String url = rawUrl.trim();
        if (YOUTUBE_ID_SHAPE.matcher(url).matches()) {
            return java.util.Optional.of(url);
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String uriPath = uri.getPath() == null ? "" : uri.getPath();
            String candidate = null;
            if (host.endsWith("youtu.be")) {
                candidate = firstPathSegment(uriPath);
            } else if (host.endsWith("youtube.com")) {
                if ("/watch".equals(uriPath)) {
                    candidate = queryParameter(uri.getRawQuery(), "v");
                } else {
                    for (String prefix : List.of("/shorts/", "/embed/", "/live/", "/v/")) {
                        if (uriPath.startsWith(prefix)) {
                            candidate = firstPathSegment(uriPath.substring(prefix.length()));
                            break;
                        }
                    }
                }
            }
            if (candidate != null && YOUTUBE_ID_SHAPE.matcher(candidate).matches()) {
                return java.util.Optional.of(candidate);
            }
        } catch (java.net.URISyntaxException ignored) {
            // fall through to empty
        }
        return java.util.Optional.empty();
    }

    private String firstPathSegment(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int separator = trimmed.indexOf('/');
        return separator < 0 ? trimmed : trimmed.substring(0, separator);
    }

    private String queryParameter(String rawQuery, String name) {
        if (rawQuery == null) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && name.equals(java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8))) {
                return java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    // ── Table ──────────────────────────────────────────────────────────────────

    private ContentBlock convertTable(GutenbergBlock block, String path, ConversionContext ctx) {
        Element root = Jsoup.parseBodyFragment(block.innerHtml(), "").body();
        Element table = root.selectFirst("table");
        if (table == null) {
            ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                    "core/table", path, !InlineHtmlExtractor.plainText(block.innerHtml()).isBlank(),
                    UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
            ));
            return null;
        }
        List<ContentBlock.TableColumn> columns = new ArrayList<>();
        Element headerRow = table.selectFirst("thead tr");
        if (headerRow != null) {
            for (Element th : headerRow.select("th,td")) {
                columns.add(new ContentBlock.TableColumn(truncate(th.text(), 60)));
            }
        }
        List<ContentBlock.TableRow> rows = new ArrayList<>();
        Elements bodyRows = table.select("tbody tr").isEmpty() ? table.select("tr") : table.select("tbody tr");
        for (Element row : bodyRows) {
            if (headerRow != null && row == headerRow) {
                continue;
            }
            List<List<InlineNode>> cells = new ArrayList<>();
            for (Element cell : row.select("td,th")) {
                cells.add(extractSafe(cell.html(), "core/table", path, ctx));
            }
            if (columns.isEmpty()) {
                for (int i = 0; i < cells.size(); i++) {
                    columns.add(new ContentBlock.TableColumn("Column " + (i + 1)));
                }
            }
            if (!cells.isEmpty()) {
                rows.add(new ContentBlock.TableRow(
                        com.brandPitara.sfs.cms.content.document.TableRowType.NORMAL, cells
                ));
            }
        }
        Element caption = table.selectFirst("caption");
        String captionText = caption == null ? null : truncate(caption.text(), 200);
        if (columns.isEmpty() || rows.isEmpty()) {
            ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                    "core/table", path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
            ));
            return null;
        }
        return new ContentBlock.Table(null, captionText, columns, rows);
    }

    // ── Containers: columns / group / column / media-text ────────────────────

    private List<ContentBlock> convertColumnsOrGroup(GutenbergBlock block, String path, ConversionContext ctx, String name) {
        List<ContentBlock> children = new ArrayList<>();
        List<GutenbergBlock> childBlocks = block.children();
        for (int i = 0; i < childBlocks.size(); i++) {
            children.addAll(convertBlock(childBlocks.get(i), path + "/" + name + "[" + i + "]", ctx));
        }
        boolean allLayoutEligible = children.size() >= 2 && children.stream().allMatch(
                c -> c instanceof ContentBlock.Image || c instanceof ContentBlock.Table
        );
        if (allLayoutEligible) {
            int columnCount = Math.min(children.size(), 3);
            List<com.brandPitara.sfs.cms.content.document.LayoutChildBlock> layoutChildren = new ArrayList<>();
            for (ContentBlock child : children) {
                layoutChildren.add((com.brandPitara.sfs.cms.content.document.LayoutChildBlock) child);
            }
            return List.of(new ContentBlock.Layout(columnCount, layoutChildren));
        }
        if (children.size() > 1) {
            ctx.warnings.add(new ConversionWarning(name, path,
                    "Flattened " + name + " into sequential blocks (mixed content is not representable as LAYOUT)."));
        }
        return children;
    }

    private List<ContentBlock> convertChildrenFlattened(GutenbergBlock block, String path, ConversionContext ctx) {
        List<ContentBlock> children = new ArrayList<>();
        List<GutenbergBlock> childBlocks = block.children();
        for (int i = 0; i < childBlocks.size(); i++) {
            children.addAll(convertBlock(childBlocks.get(i), path + "/column[" + i + "]", ctx));
        }
        return children;
    }

    private List<ContentBlock> convertMediaText(GutenbergBlock block, String path, ConversionContext ctx) {
        List<ContentBlock> result = new ArrayList<>();
        Element root = Jsoup.parseBodyFragment(block.innerHtml(), "").body();
        Element img = root.selectFirst("img");
        Long attachmentId = attachmentIdOf(block.attributes(), img);
        if (attachmentId != null) {
            ctx.referencedAttachmentIds.add(attachmentId);
            String alt = sanitizeAltText(
                    block.attributes().path("mediaAlt").asText(img == null ? "" : img.attr("alt")), "core/media-text", path, ctx
            );
            boolean decorative = alt == null || alt.isBlank();
            result.add(new ContentBlock.Image(attachmentId, decorative, decorative ? null : alt, List.of(), ImageLayout.STANDARD, null));
        }
        List<GutenbergBlock> childBlocks = block.children();
        for (int i = 0; i < childBlocks.size(); i++) {
            result.addAll(convertBlock(childBlocks.get(i), path + "/core/media-text[" + i + "]", ctx));
        }
        if (!result.isEmpty()) {
            ctx.warnings.add(new ConversionWarning("core/media-text", path,
                    "Flattened media-text side-by-side layout into sequential image + text blocks."));
        }
        return result;
    }

    // ── HTML / unknown / freeform ──────────────────────────────────────────────

    private List<ContentBlock> convertRawHtml(GutenbergBlock block, String path, ConversionContext ctx) {
        return convertHtmlLike("core/html", block.innerHtml(), path, ctx);
    }

    private List<ContentBlock> convertFreeform(GutenbergBlock block, String path, ConversionContext ctx) {
        if (block.innerHtml() == null || block.innerHtml().isBlank()) {
            return List.of();
        }
        return convertHtmlLike("(freeform)", block.innerHtml(), path, ctx);
    }

    private List<ContentBlock> convertUnknown(GutenbergBlock block, String path, ConversionContext ctx, String name) {
        boolean meaningful = !InlineHtmlExtractor.plainText(block.innerHtml()).isBlank()
                || block.children().stream().anyMatch(c -> !InlineHtmlExtractor.plainText(c.innerHtml()).isBlank());
        ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                name, path, meaningful,
                meaningful ? UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
                        : UnsupportedBlockReport.Disposition.OMITTED_NO_CONTENT
        ));
        return List.of();
    }

    private List<ContentBlock> convertHtmlLike(String label, String html, String path, ConversionContext ctx) {
        String text = InlineHtmlExtractor.plainText(html);
        if (text.isBlank()) {
            return List.of();
        }
        if (InlineHtmlExtractor.containsUnsafeMarkup(html)) {
            ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                    label, path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
            ));
            return List.of();
        }
        List<InlineNode> content = extractSafe(html, label, path, ctx);
        if (content.isEmpty()) {
            return List.of();
        }
        ctx.warnings.add(new ConversionWarning(label, path, "Converted best-effort as a plain paragraph."));
        return List.of(new ContentBlock.Paragraph(content));
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private Long attachmentIdOf(JsonNode attributes, Element img) {
        if (attributes.has("id") && attributes.get("id").isIntegralNumber()) {
            return attributes.get("id").asLong();
        }
        if (img != null) {
            return attachmentIdFromClass(img.className());
        }
        return null;
    }

    private Long attachmentIdFromClass(String className) {
        Matcher matcher = WP_IMAGE_CLASS_ID.matcher(className == null ? "" : className);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    /** Extracts inline content and immediately runs every link mark through {@link #sanitizeLinks} - the one path every text-bearing block converter uses, so no call site can forget it. */
    private List<InlineNode> extractSafe(String html, String blockName, String path, ConversionContext ctx) {
        return sanitizeLinks(InlineHtmlExtractor.extract(html), blockName, path, ctx);
    }

    /**
     * Deterministic, conservative link remediation (Section 2): a bare {@code #}/empty href or a
     * genuinely non-root-relative path is stripped to plain text with a warning (never fabricated
     * into a fake destination); a protocol-relative or plain {@code http://} URL is upgraded to
     * {@code https://} with a warning; anything else the real {@link #validator} would reject
     * (javascript:, data:, mailto:, tel:, embedded credentials, ...) is stripped with a
     * manual-review flag - visible text is always preserved, unsafe markup/URLs are never emitted.
     */
    private List<InlineNode> sanitizeLinks(List<InlineNode> nodes, String blockName, String path, ConversionContext ctx) {
        List<InlineNode> result = new ArrayList<>(nodes.size());
        for (InlineNode node : nodes) {
            if (!(node instanceof InlineNode.Text text)) {
                result.add(node);
                continue;
            }
            List<TextMark> marks = new ArrayList<>();
            for (TextMark mark : text.marks()) {
                if (!(mark instanceof TextMark.Link link)) {
                    marks.add(mark);
                    continue;
                }
                LinkDecision decision = normalizeHref(link.href());
                switch (decision.action()) {
                    case KEEP -> marks.add(link);
                    case REWRITE -> {
                        marks.add(new TextMark.Link(decision.href(), link.openInNewTab(), link.nofollow(), link.sponsored()));
                        ctx.warnings.add(new ConversionWarning(blockName, path, decision.message()));
                    }
                    case STRIP_WARN -> ctx.warnings.add(new ConversionWarning(blockName, path, decision.message()));
                    case STRIP_MANUAL_REVIEW -> {
                        ctx.warnings.add(new ConversionWarning(blockName, path, decision.message()));
                        ctx.unsupportedBlocks.add(new UnsupportedBlockReport(
                                "link", path, true, UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
                        ));
                    }
                }
            }
            result.add(new InlineNode.Text(text.text(), marks));
        }
        return result;
    }

    private enum LinkAction { KEEP, REWRITE, STRIP_WARN, STRIP_MANUAL_REVIEW }

    private record LinkDecision(LinkAction action, String href, String message) {
        static LinkDecision keep() {
            return new LinkDecision(LinkAction.KEEP, null, null);
        }
        static LinkDecision rewrite(String href, String message) {
            return new LinkDecision(LinkAction.REWRITE, href, message);
        }
        static LinkDecision stripWarn(String message) {
            return new LinkDecision(LinkAction.STRIP_WARN, null, message);
        }
        static LinkDecision stripManualReview(String message) {
            return new LinkDecision(LinkAction.STRIP_MANUAL_REVIEW, null, message);
        }
    }

    private LinkDecision normalizeHref(String href) {
        String trimmed = href == null ? "" : href.trim();
        if (trimmed.isEmpty() || trimmed.equals("#")) {
            return LinkDecision.stripWarn("Link target '#' has no real destination; hyperlink removed, text preserved.");
        }
        if (trimmed.startsWith("//")) {
            String host = hostOf("https:" + trimmed);
            if (host != null && !host.isBlank()) {
                return LinkDecision.rewrite("https:" + trimmed, "Protocol-relative link normalized to HTTPS.");
            }
            return LinkDecision.stripWarn("Protocol-relative link has no valid host; hyperlink removed, text preserved.");
        }
        if (trimmed.regionMatches(true, 0, "http://", 0, 7)) {
            return LinkDecision.rewrite("https://" + trimmed.substring(7), "Insecure http:// link upgraded to https://.");
        }
        if (trimmed.regionMatches(true, 0, "https://", 0, 8)) {
            return LinkDecision.keep(); // absolute HTTPS - the real validator still checks for embedded credentials etc.
        }
        if (trimmed.startsWith("/")) {
            return LinkDecision.keep(); // already root-relative
        }
        if (!trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            // No scheme at all - a bare relative reference (e.g. "about-us"). Best-effort,
            // deterministic normalization to root-relative, never a guessed absolute URL.
            return LinkDecision.rewrite("/" + trimmed, "Relative link normalized to a root-relative path.");
        }
        // Any other scheme (javascript:, data:, vbscript:, mailto:, tel:, file:, ...) is unsafe
        // or unsupported by the CMS link model - strip it, never pass it through.
        return LinkDecision.stripManualReview("Unsafe or unsupported link scheme removed; hyperlink stripped, text preserved.");
    }

    private String hostOf(String absoluteUrl) {
        try {
            return new java.net.URI(absoluteUrl).getHost();
        } catch (java.net.URISyntaxException exception) {
            return null;
        }
    }

    /**
     * Strips ISO control characters (a literal embedded newline/tab is the actual defect observed
     * in this dump, not raw length) and collapses whitespace, then truncates at a word boundary -
     * never mid-surrogate-pair - only if still over the CMS limit. The original value is never
     * retained here; it stays exactly where it always was, in the source dump.
     */
    private String sanitizeAltText(String rawAltText, String blockName, String path, ConversionContext ctx) {
        if (rawAltText == null) {
            return null;
        }
        StringBuilder cleaned = new StringBuilder(rawAltText.length());
        for (int i = 0; i < rawAltText.length(); i++) {
            char c = rawAltText.charAt(i);
            cleaned.append(Character.isISOControl(c) ? ' ' : c);
        }
        String collapsed = cleaned.toString().trim().replaceAll("\\s+", " ");
        String result = collapsed.length() > ContentDocumentLimits.MAX_ALT_TEXT_CHARACTERS
                ? truncateAtWordBoundary(collapsed, ContentDocumentLimits.MAX_ALT_TEXT_CHARACTERS)
                : collapsed;
        if (!result.equals(rawAltText)) {
            ctx.warnings.add(new ConversionWarning(blockName, path,
                    "Alt text normalized (original " + rawAltText.length() + " chars -> " + result.length()
                            + " chars): control characters removed"
                            + (result.length() < collapsed.length() ? " and truncated at a word boundary" : "") + "."));
        }
        return result;
    }

    private String truncateAtWordBoundary(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        int cut = maxChars;
        if (Character.isLowSurrogate(text.charAt(cut))) {
            cut--;
        }
        int lastSpace = text.lastIndexOf(' ', cut - 1);
        if (lastSpace > 0) {
            cut = lastSpace;
        }
        return text.substring(0, cut).trim();
    }

    private static final class ConversionContext {
        final List<ConversionWarning> warnings = new ArrayList<>();
        final List<ConversionBlockingError> blockingErrors = new ArrayList<>();
        final List<UnsupportedBlockReport> unsupportedBlocks = new ArrayList<>();
        final List<UnresolvedGalleryReport> unresolvedGalleries = new ArrayList<>();
        final Set<Long> referencedAttachmentIds = new LinkedHashSet<>();
        final Set<String> referencedExternalUrls = new LinkedHashSet<>();
    }
}
