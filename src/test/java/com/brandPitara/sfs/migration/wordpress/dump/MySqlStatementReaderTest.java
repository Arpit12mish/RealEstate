package com.brandPitara.sfs.migration.wordpress.dump;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlStatementReaderTest {

    @Test
    void splitsMultipleStatementsOnTopLevelSemicolons() throws IOException {
        List<String> statements = read("SELECT 1; SELECT 2; SELECT 3;");
        assertThat(statements).containsExactly("SELECT 1", " SELECT 2", " SELECT 3");
    }

    @Test
    void semicolonInsideSingleQuotedStringIsNotAStatementBoundary() throws IOException {
        List<String> statements = read("INSERT INTO t VALUES ('a;b');");
        assertThat(statements).containsExactly("INSERT INTO t VALUES ('a;b')");
    }

    @Test
    void backslashEscapedQuoteInsideStringDoesNotCloseTheString() throws IOException {
        List<String> statements = read("INSERT INTO t VALUES ('a\\'b;c');");
        assertThat(statements).containsExactly("INSERT INTO t VALUES ('a\\'b;c')");
    }

    @Test
    void doubledSingleQuoteInsideStringDoesNotCloseTheString() throws IOException {
        List<String> statements = read("INSERT INTO t VALUES ('a''b;c');");
        assertThat(statements).containsExactly("INSERT INTO t VALUES ('a''b;c')");
    }

    @Test
    void backslashEscapedBackslashIsConsumedAsAPairSoTheFollowingQuoteStillCloses() throws IOException {
        // 'a\\' is an escaped literal backslash, then the string closes; ';' after it IS a boundary.
        List<String> statements = read("INSERT INTO t VALUES ('a\\\\');SELECT 1;");
        assertThat(statements).containsExactly("INSERT INTO t VALUES ('a\\\\')", "SELECT 1");
    }

    @Test
    void semicolonInsideBacktickIdentifierIsNotABoundary() throws IOException {
        List<String> statements = read("INSERT INTO `weird;name` VALUES (1);");
        assertThat(statements).containsExactly("INSERT INTO `weird;name` VALUES (1)");
    }

    @Test
    void lineCommentDoesNotHideAStatementBoundaryOnceTheLineEnds() throws IOException {
        List<String> statements = read("-- comment; still comment\nSELECT 1;");
        assertThat(statements).containsExactly("-- comment; still comment\nSELECT 1");
    }

    @Test
    void hashLineCommentIsAlsoRecognized() throws IOException {
        List<String> statements = read("# comment; still comment\nSELECT 1;");
        assertThat(statements).containsExactly("# comment; still comment\nSELECT 1");
    }

    @Test
    void blockCommentDoesNotHideAStatementBoundaryInsideIt() throws IOException {
        List<String> statements = read("/* comment; still comment */ SELECT 1;");
        assertThat(statements).containsExactly("/* comment; still comment */ SELECT 1");
    }

    @Test
    void trailingStatementWithoutSemicolonIsStillReturned() throws IOException {
        List<String> statements = read("SELECT 1; SELECT 2");
        assertThat(statements).containsExactly("SELECT 1", " SELECT 2");
    }

    @Test
    void trailingWhitespaceOnlyFragmentIsNotReturnedAsAStatement() throws IOException {
        List<String> statements = read("SELECT 1;   \n\t ");
        assertThat(statements).containsExactly("SELECT 1");
    }

    @Test
    void emptyInputYieldsNoStatements() throws IOException {
        assertThat(read("")).isEmpty();
    }

    @Test
    void statementUnderTheConfiguredLimitParsesFine() throws IOException {
        String sql = "INSERT INTO t VALUES ('" + "x".repeat(500) + "');";
        List<String> statements = readWithLimit(sql, 1024);
        assertThat(statements).hasSize(1);
    }

    @Test
    void statementExceedingTheConfiguredMaximumFailsClosedWithoutLeakingItsContent() {
        String secretLikeValue = "password_hash_lookalike_should_never_appear_in_error";
        String sql = "INSERT INTO t VALUES ('" + secretLikeValue + "-" + "x".repeat(2000) + "');";

        assertThatThrownBy(() -> readWithLimit(sql, 1024))
                .isInstanceOf(WordPressDumpParseException.class)
                .hasMessageContaining("Statement #")
                .hasMessageContaining("1024")
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(secretLikeValue));
    }

    @Test
    void defaultLimitIsOneMebibyteAndComfortablyAboveTheObservedDumpMaximum() {
        assertThat(MySqlStatementReader.DEFAULT_MAX_STATEMENT_BYTES).isEqualTo(1024 * 1024);
    }

    private List<String> readWithLimit(String sql, int maxStatementBytes) throws IOException {
        List<String> results = new ArrayList<>();
        try (MySqlStatementReader reader = new MySqlStatementReader(
                new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)), maxStatementBytes)) {
            byte[] statement;
            while ((statement = reader.nextStatement()) != null) {
                results.add(new String(statement, StandardCharsets.UTF_8));
            }
        }
        return results;
    }

    @Test
    void unterminatedStringAtEndOfFileFailsClosed() {
        assertThatThrownBy(() -> read("INSERT INTO t VALUES ('unterminated"))
                .isInstanceOf(WordPressDumpParseException.class)
                .hasMessageContaining("unterminated");
    }

    @Test
    void unterminatedBacktickAtEndOfFileFailsClosed() {
        assertThatThrownBy(() -> read("INSERT INTO `unterminated"))
                .isInstanceOf(WordPressDumpParseException.class);
    }

    @Test
    void unterminatedBlockCommentAtEndOfFileFailsClosed() {
        assertThatThrownBy(() -> read("SELECT 1; /* never closed"))
                .isInstanceOf(WordPressDumpParseException.class);
    }

    @Test
    void multilineHtmlContentWithEmbeddedSemicolonsSurvivesIntact() throws IOException {
        String html = "<p>Hello &amp; welcome; enjoy!</p>\n<div style=\"color:red;\">Text</div>";
        List<String> statements = read("INSERT INTO t VALUES ('" + html.replace("'", "\\'") + "');");
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0)).contains(html.replace("'", "\\'"));
    }

    @Test
    void unicodeAndEmojiBytesPassThroughUnmodified() throws IOException {
        String text = "Gurgaon – café 🏠 中文";
        List<String> statements = read("INSERT INTO t VALUES ('" + text + "');");
        assertThat(statements.get(0)).contains(text);
    }

    private List<String> read(String sql) throws IOException {
        List<String> results = new ArrayList<>();
        try (MySqlStatementReader reader = new MySqlStatementReader(
                new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)))) {
            byte[] statement;
            while ((statement = reader.nextStatement()) != null) {
                results.add(new String(statement, StandardCharsets.UTF_8));
            }
        }
        return results;
    }
}
