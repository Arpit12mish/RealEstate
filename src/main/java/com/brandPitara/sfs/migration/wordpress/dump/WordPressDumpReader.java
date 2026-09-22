package com.brandPitara.sfs.migration.wordpress.dump;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streams a mysqldump export of the seven WordPress tables this migration needs, dispatching one
 * typed row at a time to a {@link WordPressDumpVisitor}. Never loads the source file into memory
 * as a whole - only the current top-level SQL statement is ever buffered (see
 * {@link MySqlStatementReader}), and a caller may bound its own memory further by discarding rows
 * it doesn't need to keep. Deliberately scoped to a single known mysqldump shape: any statement
 * that isn't a recognizable INSERT is skipped (CREATE TABLE, SET, LOCK/UNLOCK TABLES, comments),
 * but a recognized table with an unrecognized column, no column list at all, or a truncated/
 * malformed statement fails closed via {@link WordPressDumpParseException}.
 */
public final class WordPressDumpReader {

    public void read(Path path, WordPressDumpVisitor visitor) throws IOException {
        read(path, visitor, MySqlStatementReader.DEFAULT_MAX_STATEMENT_BYTES);
    }

    public void read(Path path, WordPressDumpVisitor visitor, int maxStatementBytes) throws IOException {
        try (InputStream source = WordPressDumpSource.open(path)) {
            read(source, visitor, maxStatementBytes);
        }
    }

    public void read(InputStream source, WordPressDumpVisitor visitor) throws IOException {
        read(source, visitor, MySqlStatementReader.DEFAULT_MAX_STATEMENT_BYTES);
    }

    public void read(InputStream source, WordPressDumpVisitor visitor, int maxStatementBytes) throws IOException {
        long matchedRequiredTableRows = 0;
        try (MySqlStatementReader statements = new MySqlStatementReader(source, maxStatementBytes)) {
            byte[] statementBytes;
            while ((statementBytes = statements.nextStatement()) != null) {
                long statementIndex = statements.statementIndex();
                var parsed = WordPressInsertStatement.parse(statementBytes, statementIndex);
                if (parsed.isEmpty()) {
                    continue;
                }
                WordPressInsertStatement statement = parsed.get();
                String table = statement.table();
                if (!WordPressTableSchemas.REQUIRED_TABLES.contains(table)) {
                    continue;
                }
                if (statement.columns().isEmpty()) {
                    throw WordPressDumpParseException.unsupportedDumpShape(
                            "INSERT into `" + table + "` (statement #" + statementIndex
                                    + ") has no explicit column list; this reader refuses to guess "
                                    + "positional field order."
                    );
                }
                WordPressTableSchemas.validateColumns(table, statement.columns(), statementIndex);
                for (List<String> row : statement.rows()) {
                    Map<String, String> fields = zip(statement.columns(), row);
                    dispatch(table, fields, visitor, statementIndex);
                    matchedRequiredTableRows++;
                }
            }
        }
        if (matchedRequiredTableRows == 0) {
            throw WordPressDumpParseException.unsupportedDumpShape(
                    "No rows for any of the required tables ("
                            + String.join(", ", WordPressTableSchemas.REQUIRED_TABLES)
                            + ") were found anywhere in this file - it does not look like the expected "
                            + "WordPress export."
            );
        }
    }

    private Map<String, String> zip(List<String> columns, List<String> values) {
        Map<String, String> fields = new LinkedHashMap<>(columns.size() * 2);
        for (int i = 0; i < columns.size(); i++) {
            fields.put(columns.get(i), values.get(i));
        }
        return fields;
    }

    private void dispatch(String table, Map<String, String> f, WordPressDumpVisitor visitor, long statementIndex) {
        switch (table) {
            case WordPressTableSchemas.WP_POSTS -> visitor.onPost(new WordPressPostRow(
                    requireLong(f, "ID", table, statementIndex),
                    optionalLong(f.get("post_author")),
                    f.get("post_date"), f.get("post_date_gmt"), f.get("post_modified_gmt"),
                    f.get("post_content"), f.get("post_title"), f.get("post_excerpt"),
                    f.get("post_status"), f.get("post_name"), optionalLong(f.get("post_parent")),
                    f.get("post_type"), f.get("post_mime_type"), f.get("guid")
            ));
            case WordPressTableSchemas.WP_POSTMETA -> visitor.onPostMeta(new WordPressPostMetaRow(
                    requireLong(f, "meta_id", table, statementIndex),
                    requireLong(f, "post_id", table, statementIndex),
                    f.get("meta_key"), f.get("meta_value")
            ));
            case WordPressTableSchemas.WP_TERMS -> visitor.onTerm(new WordPressTermRow(
                    requireLong(f, "term_id", table, statementIndex), f.get("name"), f.get("slug")
            ));
            case WordPressTableSchemas.WP_TERM_TAXONOMY -> visitor.onTermTaxonomy(new WordPressTermTaxonomyRow(
                    requireLong(f, "term_taxonomy_id", table, statementIndex),
                    requireLong(f, "term_id", table, statementIndex),
                    f.get("taxonomy"), optionalLong(f.get("parent"))
            ));
            case WordPressTableSchemas.WP_TERM_RELATIONSHIPS ->
                    visitor.onTermRelationship(new WordPressTermRelationshipRow(
                            requireLong(f, "object_id", table, statementIndex),
                            requireLong(f, "term_taxonomy_id", table, statementIndex)
                    ));
            case WordPressTableSchemas.WP_TERMMETA -> visitor.onTermMeta(new WordPressTermMetaRow(
                    requireLong(f, "meta_id", table, statementIndex),
                    requireLong(f, "term_id", table, statementIndex),
                    f.get("meta_key"), f.get("meta_value")
            ));
            case WordPressTableSchemas.WP_USERS -> visitor.onUser(new WordPressUserRow(
                    requireLong(f, "ID", table, statementIndex), f.get("display_name")
            ));
            default -> throw new IllegalStateException("Unreachable: " + table);
        }
    }

    private long requireLong(Map<String, String> fields, String column, String table, long statementIndex) {
        String value = fields.get(column);
        if (value == null) {
            throw WordPressDumpParseException.unsupportedDumpShape(
                    "Row in `" + table + "` (statement #" + statementIndex + ") is missing required "
                            + "identifier column `" + column + "`."
            );
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw WordPressDumpParseException.unsupportedDumpShape(
                    "Row in `" + table + "` (statement #" + statementIndex + ") has a non-numeric `"
                            + column + "`: " + value
            );
        }
    }

    private Long optionalLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
