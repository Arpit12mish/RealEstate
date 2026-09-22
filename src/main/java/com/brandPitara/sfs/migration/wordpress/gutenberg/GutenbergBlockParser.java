package com.brandPitara.sfs.migration.wordpress.gutenberg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A stateful, stack-based parser for WordPress's Gutenberg block-comment format
 * ({@code <!-- wp:name {attrs} --> ... <!-- /wp:name -->} and the self-closing
 * {@code <!-- wp:name {attrs} /-->}). Regular expressions are used only to recognize the small,
 * fixed lexical shape of a single comment delimiter; nesting (columns containing columns
 * containing paragraphs, etc.) is resolved with an explicit stack, not by a master regex - a
 * single non-greedy pattern cannot correctly bound blocks whose {@code attrs} JSON itself
 * contains nested braces (e.g. {@code core/group}'s {@code style.spacing.padding}), so the
 * attrs JSON is located with a proper balanced-brace scan and then handed to Jackson.
 */
public final class GutenbergBlockParser {

    private final ObjectMapper objectMapper;

    public GutenbergBlockParser() {
        this(new ObjectMapper());
    }

    public GutenbergBlockParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<GutenbergBlock> parse(String html) {
        if (html == null) {
            return List.of();
        }
        List<CommentToken> tokens = tokenize(html);
        return buildTree(html, tokens);
    }

    // ── Tree building ──────────────────────────────────────────────────────────

    private List<GutenbergBlock> buildTree(String html, List<CommentToken> tokens) {
        List<GutenbergBlock> topLevel = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();
        int lastEnd = 0;

        for (CommentToken token : tokens) {
            appendRawText(html.substring(lastEnd, token.start()), stack, topLevel);
            lastEnd = token.end();

            if (token.closing()) {
                Frame frame = stack.isEmpty() ? null : stack.pop();
                if (frame != null) {
                    GutenbergBlock finished = frame.toBlock();
                    addChild(stack, topLevel, finished);
                }
                continue;
            }
            if (token.selfClosing()) {
                addChild(stack, topLevel, new GutenbergBlock(token.blockName(), token.attributes(), "", List.of()));
                continue;
            }
            stack.push(new Frame(token.blockName(), token.attributes()));
        }
        appendRawText(html.substring(lastEnd), stack, topLevel);

        // Force-close any still-open frames (malformed/truncated content) rather than lose them.
        while (!stack.isEmpty()) {
            Frame frame = stack.pop();
            addChild(stack, topLevel, frame.toBlock());
        }
        return List.copyOf(topLevel);
    }

    private void appendRawText(String text, Deque<Frame> stack, List<GutenbergBlock> topLevel) {
        if (text.isEmpty()) {
            return;
        }
        if (!stack.isEmpty()) {
            stack.peek().innerHtml.append(text);
        } else if (!text.isBlank()) {
            topLevel.add(new GutenbergBlock(null, objectMapper.createObjectNode(), text, List.of()));
        }
    }

    private void addChild(Deque<Frame> stack, List<GutenbergBlock> topLevel, GutenbergBlock block) {
        if (!stack.isEmpty()) {
            stack.peek().children.add(block);
        } else {
            topLevel.add(block);
        }
    }

    private static final class Frame {
        final String blockName;
        final JsonNode attributes;
        final StringBuilder innerHtml = new StringBuilder();
        final List<GutenbergBlock> children = new ArrayList<>();

        Frame(String blockName, JsonNode attributes) {
            this.blockName = blockName;
            this.attributes = attributes;
        }

        GutenbergBlock toBlock() {
            return new GutenbergBlock(blockName, attributes, innerHtml.toString(), List.copyOf(children));
        }
    }

    // ── Tokenizing individual <!-- wp:... --> comments ────────────────────────

    private record CommentToken(
            int start, int end, boolean closing, boolean selfClosing, String blockName, JsonNode attributes
    ) {
    }

    private List<CommentToken> tokenize(String html) {
        List<CommentToken> tokens = new ArrayList<>();
        int i = 0;
        int n = html.length();
        while (i < n) {
            int commentStart = html.indexOf("<!--", i);
            if (commentStart == -1) {
                break;
            }
            int cursor = commentStart + 4;
            cursor = skipWhitespace(html, cursor);

            boolean closing = false;
            if (cursor < n && html.charAt(cursor) == '/') {
                closing = true;
                cursor++;
                cursor = skipWhitespace(html, cursor);
            }
            if (!html.startsWith("wp:", cursor)) {
                // Not a Gutenberg delimiter - an ordinary HTML comment. Skip past it as raw text.
                int commentEnd = html.indexOf("-->", commentStart);
                i = commentEnd == -1 ? n : commentEnd + 3;
                continue;
            }
            cursor += 3;
            int nameStart = cursor;
            while (cursor < n && isBlockNameChar(html.charAt(cursor))) {
                cursor++;
            }
            String blockName = html.substring(nameStart, cursor);
            cursor = skipWhitespace(html, cursor);

            JsonNode attributes = objectMapper.createObjectNode();
            if (cursor < n && html.charAt(cursor) == '{') {
                int jsonEnd = findBalancedBraceEnd(html, cursor);
                if (jsonEnd == -1) {
                    // Malformed: no balanced closing brace found. Treat the whole rest as raw
                    // text rather than guess - stop tokenizing at this comment.
                    int commentEnd = html.indexOf("-->", commentStart);
                    i = commentEnd == -1 ? n : commentEnd + 3;
                    continue;
                }
                String json = html.substring(cursor, jsonEnd + 1);
                attributes = parseAttributes(json);
                cursor = jsonEnd + 1;
                cursor = skipWhitespace(html, cursor);
            }

            boolean selfClosing = false;
            if (cursor < n && html.charAt(cursor) == '/') {
                selfClosing = true;
                cursor++;
                cursor = skipWhitespace(html, cursor);
            }
            if (!html.startsWith("-->", cursor)) {
                // Doesn't terminate the way a Gutenberg comment should - not a token we trust.
                int commentEnd = html.indexOf("-->", commentStart);
                i = commentEnd == -1 ? n : commentEnd + 3;
                continue;
            }
            int tokenEnd = cursor + 3;
            tokens.add(new CommentToken(commentStart, tokenEnd, closing, selfClosing, blockName, attributes));
            i = tokenEnd;
        }
        return tokens;
    }

    private JsonNode parseAttributes(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception malformed) {
            return objectMapper.createObjectNode();
        }
    }

    private static boolean isBlockNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '/';
    }

    private static int skipWhitespace(String html, int index) {
        while (index < html.length() && Character.isWhitespace(html.charAt(index))) {
            index++;
        }
        return index;
    }

    /** Balanced-brace scan respecting JSON string literals (so a literal {@code }} inside a
     * quoted attribute value never miscounts as structural). Returns the index of the matching
     * closing brace, or -1 if the input runs out before the braces balance. */
    private static int findBalancedBraceEnd(String html, int openBraceIndex) {
        int depth = 0;
        boolean inString = false;
        for (int i = openBraceIndex; i < html.length(); i++) {
            char c = html.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
