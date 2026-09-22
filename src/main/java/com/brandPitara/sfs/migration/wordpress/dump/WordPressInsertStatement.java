package com.brandPitara.sfs.migration.wordpress.dump;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a single top-level statement's bytes (as produced by {@link MySqlStatementReader}) as
 * a mysqldump {@code INSERT INTO `table` (`col`, ...) VALUES (...), (...), ...;} statement.
 * A statement's leading text may still carry an unterminated {@code -- comment} line glued onto
 * its front (mysqldump emits a {@code -- Dumping data for table `x`} header with no trailing
 * {@code ;}, so it becomes part of the next real statement's text once split on {@code ;}) -
 * this class looks for {@code INSERT INTO} anywhere in the statement rather than only at its
 * start, so that leading comment never causes the very first INSERT of each table to be missed.
 * <p>
 * Column values are returned already un-escaped to their logical string form (or {@code null}
 * for SQL {@code NULL} - never conflated with an empty string).
 */
public final class WordPressInsertStatement {

    private static final Pattern INSERT_HEAD = Pattern.compile(
            "INSERT INTO\\s+`?(\\w+)`?\\s*(\\(([^)]*)\\))?", Pattern.DOTALL
    );
    private static final Pattern COLUMN_NAME = Pattern.compile("`(\\w+)`");
    private static final String VALUES_KEYWORD = "VALUES";

    private final String table;
    private final List<String> columns;
    private final List<List<String>> rows;

    private WordPressInsertStatement(String table, List<String> columns, List<List<String>> rows) {
        this.table = table;
        this.columns = columns;
        this.rows = rows;
    }

    /** Returns empty if this statement is not a recognizable INSERT at all (skip, not an error). */
    public static java.util.Optional<WordPressInsertStatement> parse(byte[] statementBytes, long statementIndex) {
        String text = new String(statementBytes, StandardCharsets.UTF_8);
        Matcher headMatcher = INSERT_HEAD.matcher(text);
        if (!headMatcher.find()) {
            return java.util.Optional.empty();
        }
        String table = headMatcher.group(1);
        String columnList = headMatcher.group(3);
        List<String> columns = new ArrayList<>();
        if (columnList != null) {
            Matcher columnMatcher = COLUMN_NAME.matcher(columnList);
            while (columnMatcher.find()) {
                columns.add(columnMatcher.group(1));
            }
        }

        int valuesKeywordIndex = text.indexOf(VALUES_KEYWORD, headMatcher.end());
        if (valuesKeywordIndex == -1) {
            throw WordPressDumpParseException.missingValuesClause(statementIndex, table);
        }
        int valuesStart = valuesKeywordIndex + VALUES_KEYWORD.length();
        List<List<String>> rows = parseTuples(statementBytes, byteOffsetOf(text, valuesStart), statementIndex, table);
        if (rows.isEmpty()) {
            throw WordPressDumpParseException.missingValuesClause(statementIndex, table);
        }
        if (!columns.isEmpty()) {
            for (List<String> row : rows) {
                if (row.size() != columns.size()) {
                    throw WordPressDumpParseException.rowColumnCountMismatch(
                            statementIndex, table, columns.size(), row.size()
                    );
                }
            }
        }
        return java.util.Optional.of(new WordPressInsertStatement(table, columns, rows));
    }

    /**
     * {@code text} is a UTF-8 decoding of the same bytes the byte-offset scan below re-walks
     * directly; since mysqldump content is always valid UTF-8 and the head (table/column names)
     * is pure ASCII, the char index returned by the regex matcher equals the byte index for that
     * ASCII-only prefix.
     */
    private static int byteOffsetOf(String text, int charIndex) {
        return text.substring(0, charIndex).getBytes(StandardCharsets.UTF_8).length;
    }

    private static List<List<String>> parseTuples(
            byte[] data, int startOffset, long statementIndex, String table
    ) {
        List<List<String>> tuples = new ArrayList<>();
        List<String> currentTuple = null;
        int fieldStart = -1;
        int depth = 0;
        boolean inSingleQuote = false;
        int n = data.length;

        int i = startOffset;
        while (i < n) {
            int b = data[i] & 0xFF;
            if (inSingleQuote) {
                if (b == '\\') {
                    i += 2;
                    continue;
                }
                if (b == '\'') {
                    if (i + 1 < n && data[i + 1] == '\'') {
                        i += 2;
                        continue;
                    }
                    inSingleQuote = false;
                    i++;
                    continue;
                }
                i++;
                continue;
            }
            if (b == '\'') {
                inSingleQuote = true;
                i++;
                continue;
            }
            if (b == '(') {
                depth++;
                if (depth == 1) {
                    currentTuple = new ArrayList<>();
                    fieldStart = i + 1;
                }
                i++;
                continue;
            }
            if (b == ')') {
                depth--;
                if (depth == 0 && currentTuple != null) {
                    currentTuple.add(unescape(data, fieldStart, i));
                    tuples.add(currentTuple);
                    currentTuple = null;
                }
                i++;
                continue;
            }
            if (b == ',' && depth == 1) {
                currentTuple.add(unescape(data, fieldStart, i));
                fieldStart = i + 1;
                i++;
                continue;
            }
            i++;
        }
        if (inSingleQuote || depth != 0) {
            throw WordPressDumpParseException.unterminatedStatement(statementIndex, startOffset);
        }
        return tuples;
    }

    private static String unescape(byte[] data, int start, int end) {
        while (start < end && isSqlSpace(data[start])) start++;
        while (end > start && isSqlSpace(data[end - 1])) end--;
        if (end - start == 4
                && data[start] == 'N' && data[start + 1] == 'U' && data[start + 2] == 'L' && data[start + 3] == 'L') {
            return null;
        }
        if (end - start < 2 || data[start] != '\'' || data[end - 1] != '\'') {
            // Unquoted literal (numeric, or a bare token we don't expect but pass through raw).
            return new String(data, start, end - start, StandardCharsets.UTF_8);
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(end - start);
        int i = start + 1;
        int innerEnd = end - 1;
        while (i < innerEnd) {
            int b = data[i] & 0xFF;
            if (b == '\\' && i + 1 < innerEnd) {
                int next = data[i + 1] & 0xFF;
                switch (next) {
                    case '0' -> out.write(0);
                    case 'n' -> out.write('\n');
                    case 'r' -> out.write('\r');
                    case 't' -> out.write('\t');
                    case 'b' -> out.write('\b');
                    case 'Z' -> out.write(0x1A);
                    default -> out.write(next);
                }
                i += 2;
                continue;
            }
            if (b == '\'' && i + 1 < innerEnd && (data[i + 1] & 0xFF) == '\'') {
                out.write('\'');
                i += 2;
                continue;
            }
            out.write(b);
            i++;
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static boolean isSqlSpace(byte b) {
        return b == ' ' || b == '\n' || b == '\r' || b == '\t';
    }

    public String table() {
        return table;
    }

    public List<String> columns() {
        return columns;
    }

    public List<List<String>> rows() {
        return rows;
    }
}
