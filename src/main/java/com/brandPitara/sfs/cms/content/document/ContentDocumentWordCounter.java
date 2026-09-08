package com.brandPitara.sfs.cms.content.document;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ContentDocumentWordCounter {

    private static final Pattern WORD = Pattern.compile(
            "[\\p{L}\\p{N}]+(?:['’\u2010-\u2015-][\\p{L}\\p{N}]+)*"
    );

    public int count(ContentDocument document) {
        if (document == null || document.blocks() == null) {
            return 0;
        }
        int count = 0;
        for (ContentBlock block : document.blocks()) {
            count += countBlock(block);
        }
        return count;
    }

    private int countBlock(ContentBlock block) {
        if (block instanceof ContentBlock.Paragraph paragraph) {
            return countInline(paragraph.content());
        } else if (block instanceof ContentBlock.Heading heading) {
            return countInline(heading.content());
        } else if (block instanceof ContentBlock.Blockquote quote) {
            return countInline(quote.content());
        } else if (block instanceof ContentBlock.BulletList list) {
            return countItems(list.items());
        } else if (block instanceof ContentBlock.OrderedList list) {
            return countItems(list.items());
        } else if (block instanceof ContentBlock.Image image) {
            return countInline(image.caption());
        } else if (block instanceof ContentBlock.Video video) {
            return countInline(video.caption());
        } else if (block instanceof ContentBlock.Embed embed) {
            return countInline(embed.caption());
        } else if (block instanceof ContentBlock.CheckList checkList) {
            return countItems(checkList.items());
        } else if (block instanceof ContentBlock.Callout callout) {
            return countText(callout.title()) + countInline(callout.content());
        } else if (block instanceof ContentBlock.Table table) {
            return countTable(table);
        } else if (block instanceof ContentBlock.Layout layout) {
            // Same per-type counting a top-level IMAGE/TABLE gets - a layout child's caption/
            // cell text must contribute identically, not silently disappear from reading time.
            int total = 0;
            if (layout.children() != null) {
                for (LayoutChildBlock child : layout.children()) {
                    if (child instanceof ContentBlock.Image image) {
                        total += countInline(image.caption());
                    } else if (child instanceof ContentBlock.Table table) {
                        total += countTable(table);
                    }
                }
            }
            return total;
        }
        return 0;
    }

    private int countTable(ContentBlock.Table table) {
        int count = countText(table.title()) + countText(table.caption());
        for (ContentBlock.TableColumn column : table.columns()) {
            count += countText(column.label());
        }
        for (ContentBlock.TableRow row : table.rows()) {
            for (List<InlineNode> cell : row.cells()) {
                count += countInline(cell);
            }
        }
        return count;
    }

    private int countText(String text) {
        if (text == null) return 0;
        return (int) WORD.matcher(text).results().count();
    }

    private int countItems(List<ContentBlock.ListItem> items) {
        if (items == null) return 0;
        return items.stream().mapToInt(item -> countInline(item.content())).sum();
    }

    private int countInline(List<InlineNode> nodes) {
        if (nodes == null) return 0;
        int count = 0;
        for (InlineNode node : nodes) {
            if (node instanceof InlineNode.Text text && text.text() != null) {
                count += (int) WORD.matcher(text.text()).results().count();
            }
        }
        return count;
    }
}
