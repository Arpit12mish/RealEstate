package com.brandPitara.sfs.migration.wordpress.gutenberg;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * A single node of a parsed Gutenberg block tree. {@code blockName} is exactly as serialized in
 * the HTML comment (e.g. {@code "paragraph"}, {@code "columns"}, {@code "feedbackwp/rating-widget"}
 * - core blocks omit the {@code core/} namespace in WordPress's own serialization, third-party
 * blocks keep theirs), or {@code null} for a "freeform" fragment of raw HTML/text that sits
 * outside any block comment.
 * <p>
 * {@code innerHtml} is the raw HTML/text found directly inside this block, with any *nested*
 * block comments' own spans excluded (that content lives in {@code children} instead) - a leaf
 * block like {@code paragraph}/{@code heading}/{@code image} uses {@code innerHtml}; a container
 * block like {@code columns}/{@code column}/{@code group} uses {@code children} and its
 * {@code innerHtml} is normally just wrapper-div whitespace.
 */
public record GutenbergBlock(
        String blockName,
        JsonNode attributes,
        String innerHtml,
        List<GutenbergBlock> children
) {

    public boolean isFreeform() {
        return blockName == null;
    }

    /** Normalizes a core block's bare name ("paragraph") to its full form ("core/paragraph"); leaves third-party names untouched. */
    public String normalizedBlockName() {
        if (blockName == null) {
            return null;
        }
        return blockName.contains("/") ? blockName : "core/" + blockName;
    }
}
