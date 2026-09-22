package com.brandPitara.sfs.migration.wordpress.dump;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Streams top-level SQL statements (split on {@code ;}) out of a mysqldump export, one at a
 * time, without ever holding more than the current statement's bytes in memory. Deliberately
 * scoped to mysqldump's own quoting rules, not general SQL: single-quoted string literals with
 * backslash-escaping AND doubled {@code ''} quoting (both valid simultaneously in MySQL/MariaDB's
 * default {@code SQL_MODE}, which is what every observed dump header declares), backtick
 * identifiers, {@code --}/{@code #} line comments, and {@code /* ... *}{@code /} block comments.
 * <p>
 * Never splits on a naive {@code "),("} or per-line basis - a statement boundary is only ever a
 * literal {@code ;} byte encountered outside all of the above states, exactly mirroring how
 * mysqldump's own escaping guarantees a statement can be re-parsed unambiguously.
 */
public final class MySqlStatementReader implements AutoCloseable {

    private static final int BACKSLASH = '\\';
    private static final int SINGLE_QUOTE = '\'';
    private static final int BACKTICK = '`';
    private static final int DASH = '-';
    private static final int HASH = '#';
    private static final int SLASH = '/';
    private static final int STAR = '*';
    private static final int SEMICOLON = ';';
    private static final int NEWLINE = '\n';

    /** Safely above the largest statement actually observed in this dump shape (~280 KiB). */
    public static final int DEFAULT_MAX_STATEMENT_BYTES = 1024 * 1024;

    private final PushbackInputStream input;
    private final int maxStatementBytes;
    private long statementIndex = 0;
    private long byteOffset = 0;
    private boolean closed = false;

    public MySqlStatementReader(InputStream input) {
        this(input, DEFAULT_MAX_STATEMENT_BYTES);
    }

    public MySqlStatementReader(InputStream input, int maxStatementBytes) {
        this.input = new PushbackInputStream(input, 1);
        this.maxStatementBytes = maxStatementBytes;
    }

    /**
     * Returns the next statement's raw bytes (without the trailing {@code ;}), or {@code null}
     * at end of input. A trailing fragment with no terminating {@code ;} is returned as-is if it
     * contains anything other than whitespace, so a genuinely truncated file still surfaces its
     * last partial statement rather than silently vanishing.
     */
    public byte[] nextStatement() throws IOException {
        if (closed) {
            throw new IllegalStateException("Reader is closed.");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
        long statementStartOffset = byteOffset;
        boolean inSingleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean any = false;

        int current;
        while ((current = read()) != -1) {
            any = true;
            if (buffer.size() > maxStatementBytes) {
                throw WordPressDumpParseException.statementTooLarge(
                        statementIndex, statementStartOffset, maxStatementBytes
                );
            }

            if (inLineComment) {
                buffer.write(current);
                if (current == NEWLINE) {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                buffer.write(current);
                if (current == STAR && consumeIfNext(buffer, SLASH)) {
                    inBlockComment = false;
                }
                continue;
            }
            if (inSingleQuote) {
                if (current == BACKSLASH) {
                    buffer.write(current);
                    int escaped = read();
                    if (escaped == -1) {
                        throw WordPressDumpParseException.unterminatedStatement(statementIndex, statementStartOffset);
                    }
                    buffer.write(escaped);
                    continue;
                }
                if (current == SINGLE_QUOTE) {
                    buffer.write(current);
                    if (!consumeIfNext(buffer, SINGLE_QUOTE)) {
                        inSingleQuote = false;
                    }
                    continue;
                }
                buffer.write(current);
                continue;
            }
            if (inBacktick) {
                buffer.write(current);
                if (current == BACKTICK) {
                    inBacktick = false;
                }
                continue;
            }

            // Not inside any special state.
            if (current == SINGLE_QUOTE) {
                inSingleQuote = true;
                buffer.write(current);
            } else if (current == BACKTICK) {
                inBacktick = true;
                buffer.write(current);
            } else if (current == DASH) {
                buffer.write(current);
                if (consumeIfNext(buffer, DASH)) {
                    inLineComment = true;
                }
            } else if (current == HASH) {
                inLineComment = true;
                buffer.write(current);
            } else if (current == SLASH) {
                buffer.write(current);
                if (consumeIfNext(buffer, STAR)) {
                    inBlockComment = true;
                }
            } else if (current == SEMICOLON) {
                statementIndex++;
                return buffer.toByteArray();
            } else {
                buffer.write(current);
            }
        }

        if (inSingleQuote || inBacktick || inBlockComment) {
            throw WordPressDumpParseException.unterminatedStatement(statementIndex, statementStartOffset);
        }
        if (!any) {
            return null;
        }
        byte[] tail = buffer.toByteArray();
        if (isBlank(tail)) {
            return null;
        }
        statementIndex++;
        return tail;
    }

    public long statementIndex() {
        return statementIndex;
    }

    private int read() throws IOException {
        int b = input.read();
        if (b != -1) {
            byteOffset++;
        }
        return b;
    }

    /**
     * Peeks the next byte; if it equals {@code expected}, writes it to the buffer and returns
     * true, otherwise pushes it back unread and returns false. Using a real pushback stream here
     * (rather than a hand-rolled "pending byte" flag) guarantees a byte that turns out NOT to
     * match is still seen by the very next {@link #read()} call, in the correct outer state.
     */
    private boolean consumeIfNext(ByteArrayOutputStream buffer, int expected) throws IOException {
        int next = input.read();
        if (next == -1) {
            return false;
        }
        if (next == expected) {
            byteOffset++;
            buffer.write(next);
            return true;
        }
        input.unread(next);
        return false;
    }

    private static boolean isBlank(byte[] bytes) {
        for (byte b : bytes) {
            if (b != ' ' && b != '\t' && b != '\r' && b != '\n') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        input.close();
    }
}
