package com.brandPitara.sfs.migration.wordpress.dump;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordPressDumpReaderTest {

    private final WordPressDumpReader reader = new WordPressDumpReader();

    @Test
    void parsesBatchedMultiRowInsertForPostmeta() throws IOException {
        List<WordPressPostMetaRow> rows = new ArrayList<>();
        read("""
                INSERT INTO `wp_postmeta` (`meta_id`, `post_id`, `meta_key`, `meta_value`) VALUES
                (1, 100, '_thumbnail_id', '55'),
                (2, 100, '_edit_last', '1'),
                (3, 101, '_thumbnail_id', '56');
                """, new WordPressDumpVisitor() {
            @Override
            public void onPostMeta(WordPressPostMetaRow row) {
                rows.add(row);
            }
        });

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).isEqualTo(new WordPressPostMetaRow(1, 100, "_thumbnail_id", "55"));
        assertThat(rows.get(2)).isEqualTo(new WordPressPostMetaRow(3, 101, "_thumbnail_id", "56"));
    }

    @Test
    void parsesHtmlContentWithCommasParenthesesAndEscapedQuotes() throws IOException {
        String html = "<p>Prices start at \\'\u20b91.2 Cr\\' (approx.), includes GST, registration, and stamp duty.</p>";
        List<WordPressPostRow> rows = new ArrayList<>();
        read("""
                INSERT INTO `wp_posts` (`ID`, `post_author`, `post_date`, `post_date_gmt`, `post_content`,
                    `post_title`, `post_excerpt`, `post_status`, `comment_status`, `ping_status`,
                    `post_password`, `post_name`, `to_ping`, `pinged`, `post_modified`, `post_modified_gmt`,
                    `post_content_filtered`, `post_parent`, `guid`, `menu_order`, `post_type`,
                    `post_mime_type`, `comment_count`) VALUES
                (1, 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00', '%s', 'Title, With Comma (and parens)',
                    '', 'publish', 'closed', 'closed', '', 'slug-1', '', '', '2024-01-01 00:00:00',
                    '2024-01-01 00:00:00', '', 0, 'https://example.com/?p=1', 0, 'post', '', 0);
                """.formatted(html), new WordPressDumpVisitor() {
            @Override
            public void onPost(WordPressPostRow row) {
                rows.add(row);
            }
        });

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).postContent()).isEqualTo(html.replace("\\'", "'"));
        assertThat(rows.get(0).postTitle()).isEqualTo("Title, With Comma (and parens)");
    }

    @Test
    void distinguishesSqlNullFromEmptyString() throws IOException {
        List<WordPressPostMetaRow> rows = new ArrayList<>();
        read("""
                INSERT INTO `wp_postmeta` (`meta_id`, `post_id`, `meta_key`, `meta_value`) VALUES
                (1, 100, '_empty', ''),
                (2, 100, '_null', NULL);
                """, new WordPressDumpVisitor() {
            @Override
            public void onPostMeta(WordPressPostMetaRow row) {
                rows.add(row);
            }
        });

        assertThat(rows.get(0).metaValue()).isEqualTo("");
        assertThat(rows.get(1).metaValue()).isNull();
    }

    @Test
    void preservesSerializedPhpMetadataVerbatim() throws IOException {
        String serialized = "a:3:{s:5:\\\"width\\\";i:1024;s:6:\\\"height\\\";i:768;s:4:\\\"file\\\";s:9:\\\"photo.jpg\\\";}";
        List<WordPressPostMetaRow> rows = new ArrayList<>();
        read("""
                INSERT INTO `wp_postmeta` (`meta_id`, `post_id`, `meta_key`, `meta_value`) VALUES
                (1, 100, '_wp_attachment_metadata', '%s');
                """.formatted(serialized), new WordPressDumpVisitor() {
            @Override
            public void onPostMeta(WordPressPostMetaRow row) {
                rows.add(row);
            }
        });

        assertThat(rows.get(0).metaValue()).isEqualTo(serialized.replace("\\\"", "\""));
    }

    @Test
    void preservesUnicodeAndEmoji() throws IOException {
        String title = "Gurgaon \u2013 caf\u00e9 living \ud83c\udfe0 \u4e2d\u6587\u6807\u9898";
        List<WordPressPostRow> rows = new ArrayList<>();
        read(minimalPostInsert(1, title, "publish"), new WordPressDumpVisitor() {
            @Override
            public void onPost(WordPressPostRow row) {
                rows.add(row);
            }
        });
        assertThat(rows.get(0).postTitle()).isEqualTo(title);
    }

    @Test
    void reorderedColumnsStillMapToTheCorrectFieldByName() throws IOException {
        List<WordPressTermRow> rows = new ArrayList<>();
        read("""
                INSERT INTO `wp_terms` (`slug`, `term_id`, `name`, `term_group`) VALUES
                ('real-estate', 9, 'Real Estate', 0);
                """, new WordPressDumpVisitor() {
            @Override
            public void onTerm(WordPressTermRow row) {
                rows.add(row);
            }
        });

        assertThat(rows.get(0)).isEqualTo(new WordPressTermRow(9, "Real Estate", "real-estate"));
    }

    @Test
    void unknownColumnFailsClosedRatherThanGuessing() {
        assertThatThrownBy(() -> read("""
                INSERT INTO `wp_posts` (`ID`, `post_title`, `made_up_column`) VALUES (1, 'x', 'y');
                """, new WordPressDumpVisitor() {
        }))
                .isInstanceOf(WordPressDumpParseException.class)
                .hasMessageContaining("made_up_column")
                .hasMessageContaining("wp_posts");
    }

    @Test
    void insertWithNoExplicitColumnListFailsClosed() {
        assertThatThrownBy(() -> read("""
                INSERT INTO `wp_posts` VALUES (1, 'x');
                """, new WordPressDumpVisitor() {
        }))
                .isInstanceOf(WordPressDumpParseException.class);
    }

    @Test
    void insertWithNoValuesClauseFailsClosedWithLocation() {
        assertThatThrownBy(() -> read("""
                INSERT INTO `wp_posts` (`ID`) SELECT id FROM other_table;
                """, new WordPressDumpVisitor() {
        }))
                .isInstanceOf(WordPressDumpParseException.class)
                .hasMessageContaining("wp_posts")
                .hasMessageContaining("VALUES");
    }

    @Test
    void rowWithFewerFieldsThanDeclaredColumnsFailsClosed() {
        assertThatThrownBy(() -> read("""
                INSERT INTO `wp_terms` (`term_id`, `name`, `slug`) VALUES (1, 'Only two fields');
                """, new WordPressDumpVisitor() {
        }))
                .isInstanceOf(WordPressDumpParseException.class);
    }

    @Test
    void nonInsertStatementsAreSkippedNotTreatedAsErrors() throws IOException {
        List<WordPressPostRow> rows = new ArrayList<>();
        read("""
                CREATE TABLE `wp_posts` (`ID` bigint(20) NOT NULL);
                SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
                LOCK TABLES `wp_posts` WRITE;
                %s
                UNLOCK TABLES;
                """.formatted(minimalPostInsert(1, "Only real statement", "publish")), new WordPressDumpVisitor() {
            @Override
            public void onPost(WordPressPostRow row) {
                rows.add(row);
            }
        });
        assertThat(rows).hasSize(1);
    }

    @Test
    void unrelatedTableIsIgnoredWithoutError() throws IOException {
        List<WordPressPostRow> rows = new ArrayList<>();
        read("""
                INSERT INTO `wp_options` (`option_id`, `option_name`) VALUES (1, 'siteurl');
                %s
                """.formatted(minimalPostInsert(1, "Real post", "publish")), new WordPressDumpVisitor() {
            @Override
            public void onPost(WordPressPostRow row) {
                rows.add(row);
            }
        });
        assertThat(rows).hasSize(1);
    }

    @Test
    void fileWithNoRecognizedTableAtAllFailsClosedAsUnsupportedShape() {
        assertThatThrownBy(() -> read("""
                INSERT INTO `wp_options` (`option_id`, `option_name`) VALUES (1, 'siteurl');
                """, new WordPressDumpVisitor() {
        }))
                .isInstanceOf(WordPressDumpParseException.class)
                .hasMessageContaining("does not look like");
    }

    @Test
    void wordPressUserRowExposesOnlyIdAndDisplayNameNeverCredentials() throws IOException {
        List<WordPressUserRow> rows = new ArrayList<>();
        read("""
                INSERT INTO `wp_users` (`ID`, `user_login`, `user_pass`, `user_nicename`, `user_email`,
                    `user_url`, `user_registered`, `user_activation_key`, `user_status`, `display_name`)
                    VALUES (1, 'admin', '$P$Bsecrethash', 'admin', 'admin@example.com', '',
                    '2020-01-01 00:00:00', '', 0, 'Square Foot Story');
                """, new WordPressDumpVisitor() {
            @Override
            public void onUser(WordPressUserRow row) {
                rows.add(row);
            }
        });

        assertThat(rows).containsExactly(new WordPressUserRow(1, "Square Foot Story"));
        // WordPressUserRow's own declared fields are exhaustively id+displayName - there is no
        // getter, field, or accessor through which a password hash could ever reach a caller.
        assertThat(WordPressUserRow.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("id", "displayName");
    }

    @Test
    void gzipCompressedDumpParsesIdenticallyToPlainText() throws IOException {
        String sql = minimalPostInsert(1, "Gzip source post", "publish");
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(sql.getBytes(StandardCharsets.UTF_8));
        }

        List<WordPressPostRow> rows = new ArrayList<>();
        reader.read(new java.util.zip.GZIPInputStream(
                new ByteArrayInputStream(compressed.toByteArray())
        ), new WordPressDumpVisitor() {
            @Override
            public void onPost(WordPressPostRow row) {
                rows.add(row);
            }
        });

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).postTitle()).isEqualTo("Gzip source post");
    }

    @Test
    void readsAPlainSqlFileFromDiskByPath(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir)
            throws IOException {
        java.nio.file.Path sqlFile = tempDir.resolve("dump.sql");
        java.nio.file.Files.writeString(sqlFile, minimalPostInsert(1, "From disk", "publish"));

        List<WordPressPostRow> rows = new ArrayList<>();
        reader.read(sqlFile, new WordPressDumpVisitor() {
            @Override
            public void onPost(WordPressPostRow row) {
                rows.add(row);
            }
        });
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).postTitle()).isEqualTo("From disk");
    }

    @Test
    void readsAGzipCompressedSqlGzFileFromDiskByPath(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir)
            throws IOException {
        java.nio.file.Path gzFile = tempDir.resolve("dump.sql.gz");
        try (var out = new GZIPOutputStream(java.nio.file.Files.newOutputStream(gzFile))) {
            out.write(minimalPostInsert(1, "From gz on disk", "publish").getBytes(StandardCharsets.UTF_8));
        }

        List<WordPressPostRow> rows = new ArrayList<>();
        reader.read(gzFile, new WordPressDumpVisitor() {
            @Override
            public void onPost(WordPressPostRow row) {
                rows.add(row);
            }
        });
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).postTitle()).isEqualTo("From gz on disk");
    }

    @Test
    void malformedTrailingStatementFailsClosedWithUsefulLocation() {
        assertThatThrownBy(() -> read("""
                %s
                INSERT INTO `wp_posts` (`ID`, `post_title`) VALUES (2, 'unterminated
                """.formatted(minimalPostInsert(1, "First post", "publish")), new WordPressDumpVisitor() {
        }))
                .isInstanceOf(WordPressDumpParseException.class)
                .hasMessageContaining("statement #");
    }

    private String minimalPostInsert(long id, String title, String status) {
        return """
                INSERT INTO `wp_posts` (`ID`, `post_author`, `post_date`, `post_date_gmt`, `post_content`,
                    `post_title`, `post_excerpt`, `post_status`, `comment_status`, `ping_status`,
                    `post_password`, `post_name`, `to_ping`, `pinged`, `post_modified`, `post_modified_gmt`,
                    `post_content_filtered`, `post_parent`, `guid`, `menu_order`, `post_type`,
                    `post_mime_type`, `comment_count`) VALUES
                (%d, 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00', '<p>Body</p>', '%s', '', '%s',
                    'closed', 'closed', '', 'slug', '', '', '2024-01-01 00:00:00', '2024-01-01 00:00:00',
                    '', 0, 'https://example.com/?p=%d', 0, 'post', '', 0);
                """.formatted(id, title, status, id);
    }

    private void read(String sql, WordPressDumpVisitor visitor) throws IOException {
        reader.read(new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)), visitor);
    }
}
