package com.brandPitara.sfs.migration.wordpress.dump;

/**
 * Thrown when the source file does not match the specific mysqldump shape this reader is
 * deliberately scoped to (see {@link MySqlStatementReader}/{@link WordPressDumpReader}) - an
 * unterminated quoted string, an INSERT with no recognizable VALUES clause, or a column name this
 * reader does not know for one of the seven required tables. Always carries enough location
 * context (statement index, byte offset, table name where known) to find the offending statement
 * without re-reading the whole file by hand.
 */
public class WordPressDumpParseException extends RuntimeException {

    public WordPressDumpParseException(String message) {
        super(message);
    }

    public static WordPressDumpParseException unterminatedStatement(long statementIndex, long byteOffset) {
        return new WordPressDumpParseException(
                "Dump ended mid-statement (unterminated quoted string or comment) - statement #"
                        + statementIndex + " starting near byte offset " + byteOffset
                        + ". This reader only supports a well-formed mysqldump export; the file may be "
                        + "truncated or use an unsupported escaping mode."
        );
    }

    public static WordPressDumpParseException missingValuesClause(long statementIndex, String table) {
        return new WordPressDumpParseException(
                "INSERT statement #" + statementIndex + " for table `" + table + "` has no recognizable "
                        + "VALUES clause. This reader only supports the standard "
                        + "\"INSERT INTO `table` (`col`, ...) VALUES (...), (...);\" mysqldump shape."
        );
    }

    public static WordPressDumpParseException unknownColumn(long statementIndex, String table, String column) {
        return new WordPressDumpParseException(
                "INSERT statement #" + statementIndex + " for table `" + table + "` declares column `"
                        + column + "` which this reader does not recognize. Failing closed rather than "
                        + "risk silently mis-mapping a positional value - add the column to "
                        + "WordPressTableSchemas if it is genuinely expected."
        );
    }

    public static WordPressDumpParseException rowColumnCountMismatch(
            long statementIndex, String table, int expectedColumns, int actualFields
    ) {
        return new WordPressDumpParseException(
                "INSERT statement #" + statementIndex + " for table `" + table + "` declares "
                        + expectedColumns + " columns but a row tuple has " + actualFields + " fields."
        );
    }

    public static WordPressDumpParseException statementTooLarge(
            long statementIndex, long byteOffset, int maxStatementBytes
    ) {
        return new WordPressDumpParseException(
                "Statement #" + statementIndex + " starting near byte offset " + byteOffset
                        + " exceeds the configured maximum of " + maxStatementBytes + " bytes. "
                        + "This is a safety limit, not a value dump - no statement content is included here."
        );
    }

    public static WordPressDumpParseException unsupportedDumpShape(String detail) {
        return new WordPressDumpParseException(
                "Source file does not look like the supported mysqldump export shape: " + detail
        );
    }
}
