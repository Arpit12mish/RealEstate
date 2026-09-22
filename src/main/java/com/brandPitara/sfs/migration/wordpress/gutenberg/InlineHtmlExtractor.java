package com.brandPitara.sfs.migration.wordpress.gutenberg;

import com.brandPitara.sfs.cms.content.document.InlineNode;
import com.brandPitara.sfs.cms.content.document.TextMark;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts a flat, safe {@link InlineNode} run from a fragment of rendered WordPress HTML
 * (a paragraph/heading/list-item/table-cell's inner markup). Only the small set of formatting
 * semantics {@link com.brandPitara.sfs.cms.content.document.TextMark} actually supports is
 * recognized ({@code strong/b}, {@code em/i}, {@code u}, {@code a[href]}); everything else
 * (spans, classes, inline styles, data attributes, scripts, iframes, forms) is unwrapped to its
 * plain text - never passed through as markup. {@code <br>} becomes {@link InlineNode.HardBreak}.
 * Final link-safety (scheme, credentials, control characters) is left to
 * {@code ContentDocumentValidator}, which every converted document is run through before being
 * treated as a candidate for real content - this class only avoids emitting raw HTML as text.
 */
final class InlineHtmlExtractor {

    private static final Set<String> UNSAFE_TAGS = Set.of("script", "style", "iframe", "form", "object", "embed");

    private InlineHtmlExtractor() {
    }

    static List<InlineNode> extract(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Element body = Jsoup.parseBodyFragment(html, "").body();
        List<InlineNode> nodes = new ArrayList<>();
        walk(body, nodes, new LinkedHashSet<>());
        return trimEdges(coalesce(nodes));
    }

    /** Plain visible text only, used for "does this block have meaningful content" checks. */
    static String plainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parseBodyFragment(html, "").body().text();
    }

    static boolean containsUnsafeMarkup(String html) {
        if (html == null) {
            return false;
        }
        // parseBodyFragment (not a full-document parse) so a leading <script>/<iframe> stays a
        // literal child here instead of being HTML5-relocated into a synthesized <head>.
        Element body = Jsoup.parseBodyFragment(html, "").body();
        for (String tag : UNSAFE_TAGS) {
            if (!body.getElementsByTag(tag).isEmpty()) {
                return true;
            }
        }
        return !body.select("[onclick],[onerror],[onload]").isEmpty();
    }

    private static void walk(Node node, List<InlineNode> out, Set<TextMark> activeMarks) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.text();
                if (!text.isEmpty()) {
                    out.add(new InlineNode.Text(text, List.copyOf(activeMarks)));
                }
            } else if (child instanceof Element element) {
                String tag = element.tagName().toLowerCase();
                switch (tag) {
                    case "br" -> out.add(new InlineNode.HardBreak());
                    case "strong", "b" -> walkWithMark(element, out, activeMarks, new TextMark.Bold());
                    case "em", "i" -> walkWithMark(element, out, activeMarks, new TextMark.Italic());
                    case "u" -> walkWithMark(element, out, activeMarks, new TextMark.Underline());
                    case "a" -> {
                        String href = element.attr("href");
                        if (href == null || href.isBlank()) {
                            walk(element, out, activeMarks);
                        } else {
                            walkWithMark(element, out, activeMarks, new TextMark.Link(
                                    href, "_blank".equalsIgnoreCase(element.attr("target")),
                                    "nofollow".equalsIgnoreCase(element.attr("rel")), false
                            ));
                        }
                    }
                    default -> walk(element, out, activeMarks);
                }
            }
        }
    }

    private static void walkWithMark(Element element, List<InlineNode> out, Set<TextMark> activeMarks, TextMark mark) {
        Set<TextMark> withMark = new LinkedHashSet<>(activeMarks);
        withMark.removeIf(existing -> existing.getClass() == mark.getClass());
        withMark.add(mark);
        walk(element, out, withMark);
    }

    /** Merges consecutive text nodes carrying identical marks, so round-tripping stays stable. */
    private static List<InlineNode> coalesce(List<InlineNode> nodes) {
        List<InlineNode> merged = new ArrayList<>();
        for (InlineNode node : nodes) {
            if (node instanceof InlineNode.Text text
                    && !merged.isEmpty()
                    && merged.get(merged.size() - 1) instanceof InlineNode.Text previous
                    && previous.marks().equals(text.marks())) {
                merged.set(merged.size() - 1, new InlineNode.Text(previous.text() + text.text(), previous.marks()));
            } else {
                merged.add(node);
            }
        }
        return merged;
    }

    /**
     * Strips purely-cosmetic leading/trailing whitespace introduced by WordPress's own
     * pretty-printed block markup (the newline/indentation between a block comment and its
     * inner tag, e.g. {@code "\n<li>First</li>\n"}) - never internal whitespace, which is
     * genuine content ("Read our guide" keeps its spaces).
     */
    private static List<InlineNode> trimEdges(List<InlineNode> nodes) {
        if (nodes.isEmpty()) {
            return nodes;
        }
        List<InlineNode> result = new ArrayList<>(nodes);
        if (result.get(0) instanceof InlineNode.Text first) {
            result.set(0, new InlineNode.Text(first.text().stripLeading(), first.marks()));
        }
        int lastIndex = result.size() - 1;
        if (result.get(lastIndex) instanceof InlineNode.Text last) {
            result.set(lastIndex, new InlineNode.Text(last.text().stripTrailing(), last.marks()));
        }
        result.removeIf(node -> node instanceof InlineNode.Text text && text.text().isEmpty());
        return result;
    }
}
