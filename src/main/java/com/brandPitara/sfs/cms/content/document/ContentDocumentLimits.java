package com.brandPitara.sfs.cms.content.document;

public final class ContentDocumentLimits {

    public static final int MAX_SERIALIZED_BYTES = 768 * 1024;
    public static final int MAX_REQUEST_BYTES = 1024 * 1024;
    public static final int MAX_BLOCKS = 750;
    public static final int MAX_INLINE_NODES_PER_BLOCK = 500;
    public static final int MAX_LIST_ITEMS = 200;
    public static final int MAX_INLINE_NODES_PER_LIST_ITEM = 100;
    public static final int MAX_TEXT_NODE_CHARACTERS = 20_000;
    public static final int MAX_TOTAL_TEXT_CHARACTERS = 500_000;
    public static final int MAX_MARKS_PER_TEXT_NODE = 4;
    public static final int MAX_ALT_TEXT_CHARACTERS = 300;
    public static final int MAX_CAPTION_INLINE_NODES = 50;
    public static final int MAX_CAPTION_CHARACTERS = 1_000;

    // v3: CHECK_LIST reuses MAX_LIST_ITEMS / MAX_INLINE_NODES_PER_LIST_ITEM — same shape as
    // BULLET_LIST/ORDERED_LIST items, so no dedicated constants are needed.
    public static final int MAX_CALLOUT_TITLE_CHARACTERS = 120;
    public static final int MAX_TABLE_COLUMNS = 8;
    public static final int MAX_TABLE_ROWS = 100;
    public static final int MAX_TABLE_CAPTION_CHARACTERS = 200;
    public static final int MAX_TABLE_COLUMN_LABEL_CHARACTERS = 60;
    public static final int MAX_TABLE_CELL_INLINE_NODES = 20;

    // v4: LAYOUT (see ContentBlock.Layout / LayoutChildBlock) and TABLE.title.
    public static final int MIN_LAYOUT_COLUMNS = 1;
    public static final int MAX_LAYOUT_COLUMNS = 3;
    public static final int MAX_LAYOUT_CHILDREN = 12;
    public static final int MAX_TABLE_TITLE_CHARACTERS = 120;

    // v5: IMAGE_GALLERY (see ContentBlock.Gallery). Column range is one wider than LAYOUT's
    // (up to 4) since a gallery is a dedicated, denser photo grid rather than a general content
    // section. 40 images is generous headroom over every real gallery observed so far (max 6).
    public static final int MIN_GALLERY_COLUMNS = 1;
    public static final int MAX_GALLERY_COLUMNS = 4;
    public static final int MAX_GALLERY_IMAGES = 40;

    private ContentDocumentLimits() {
    }
}
