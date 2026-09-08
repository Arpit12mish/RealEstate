package com.brandPitara.sfs.cms.content.document;

/**
 * A table's header row is represented by {@link ContentBlock.TableColumn#label()}, not a
 * row type, to avoid two ways of expressing the same "this is the header" concept.
 */
public enum TableRowType {
    NORMAL,
    SECTION,
    TOTAL
}
