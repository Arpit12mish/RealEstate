package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.InlineNode;
import com.brandPitara.sfs.cms.content.domain.ContentValidation;

import java.util.List;

/**
 * Generates a deterministic plain-text excerpt from a converted document's own visible text -
 * never an external AI service, never text unrelated to the source. Stops at a sentence boundary
 * when one exists within the limit, otherwise a word boundary; never splits mid-word.
 */
public final class WordPressExcerptGenerator {

    private static final int TARGET_LENGTH = 160;

    private WordPressExcerptGenerator() {
    }

    public static String generate(ContentDocument document) {
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : document.blocks()) {
            appendBlockText(block, text);
            if (text.length() >= TARGET_LENGTH * 2) {
                break;
            }
        }
        String collapsed = text.toString().trim().replaceAll("\\s+", " ");
        if (collapsed.isEmpty()) {
            return null;
        }
        if (collapsed.length() <= ContentValidation.EXCERPT_MAX && collapsed.length() <= TARGET_LENGTH) {
            return collapsed;
        }
        int limit = Math.min(TARGET_LENGTH, ContentValidation.EXCERPT_MAX);
        if (collapsed.length() <= limit) {
            return collapsed;
        }
        String truncated = collapsed.substring(0, limit);
        int sentenceEnd = lastIndexOfAny(truncated, '.', '!', '?');
        if (sentenceEnd > limit / 2) {
            return truncated.substring(0, sentenceEnd + 1).trim();
        }
        int lastSpace = truncated.lastIndexOf(' ');
        return (lastSpace > 0 ? truncated.substring(0, lastSpace) : truncated).trim();
    }

    private static void appendBlockText(ContentBlock block, StringBuilder text) {
        if (block instanceof ContentBlock.Paragraph paragraph) {
            appendInline(paragraph.content(), text);
        } else if (block instanceof ContentBlock.Heading heading) {
            appendInline(heading.content(), text);
        } else if (block instanceof ContentBlock.Blockquote quote) {
            appendInline(quote.content(), text);
        }
        if (text.length() > 0 && text.charAt(text.length() - 1) != ' ') {
            text.append(' ');
        }
    }

    private static void appendInline(List<InlineNode> nodes, StringBuilder text) {
        for (InlineNode node : nodes) {
            if (node instanceof InlineNode.Text t) {
                text.append(t.text());
            }
        }
    }

    private static int lastIndexOfAny(String text, char... candidates) {
        int best = -1;
        for (char c : candidates) {
            best = Math.max(best, text.lastIndexOf(c));
        }
        return best;
    }
}
