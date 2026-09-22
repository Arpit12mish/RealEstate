package com.brandPitara.sfs.migration.wordpress.dump;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Known column names per required WordPress table, as declared by a standard WordPress core
 * schema export. An INSERT declaring a column outside this set fails closed
 * ({@link WordPressDumpParseException#unknownColumn}) rather than risk silently mis-mapping a
 * positional value from an unexpected dump shape (e.g. a different WordPress core version, or a
 * plugin that added a column to one of these tables).
 */
public final class WordPressTableSchemas {

    public static final String WP_POSTS = "wp_posts";
    public static final String WP_POSTMETA = "wp_postmeta";
    public static final String WP_TERMS = "wp_terms";
    public static final String WP_TERM_TAXONOMY = "wp_term_taxonomy";
    public static final String WP_TERM_RELATIONSHIPS = "wp_term_relationships";
    public static final String WP_TERMMETA = "wp_termmeta";
    public static final String WP_USERS = "wp_users";

    public static final Set<String> REQUIRED_TABLES = Set.of(
            WP_POSTS, WP_POSTMETA, WP_TERMS, WP_TERM_TAXONOMY,
            WP_TERM_RELATIONSHIPS, WP_TERMMETA, WP_USERS
    );

    private static final Map<String, Set<String>> KNOWN_COLUMNS = Map.of(
            WP_POSTS, Set.of(
                    "ID", "post_author", "post_date", "post_date_gmt", "post_content", "post_title",
                    "post_excerpt", "post_status", "comment_status", "ping_status", "post_password",
                    "post_name", "to_ping", "pinged", "post_modified", "post_modified_gmt",
                    "post_content_filtered", "post_parent", "guid", "menu_order", "post_type",
                    "post_mime_type", "comment_count"
            ),
            WP_POSTMETA, Set.of("meta_id", "post_id", "meta_key", "meta_value"),
            WP_TERMS, Set.of("term_id", "name", "slug", "term_group"),
            WP_TERM_TAXONOMY, Set.of("term_taxonomy_id", "term_id", "taxonomy", "description", "parent", "count"),
            WP_TERM_RELATIONSHIPS, Set.of("object_id", "term_taxonomy_id", "term_order"),
            WP_TERMMETA, Set.of("meta_id", "term_id", "meta_key", "meta_value"),
            WP_USERS, Set.of(
                    "ID", "user_login", "user_pass", "user_nicename", "user_email", "user_url",
                    "user_registered", "user_activation_key", "user_status", "display_name"
            )
    );

    /** wp_users columns this reader will never retain past the moment of parsing a row. */
    public static final Set<String> WP_USERS_AUTHENTICATION_COLUMNS = Set.of(
            "user_login", "user_pass", "user_email", "user_url", "user_registered",
            "user_activation_key", "user_status", "user_nicename"
    );

    private WordPressTableSchemas() {
    }

    public static void validateColumns(String table, List<String> declaredColumns, long statementIndex) {
        Set<String> known = KNOWN_COLUMNS.get(table);
        if (known == null) {
            return;
        }
        for (String column : declaredColumns) {
            if (!known.contains(column)) {
                throw WordPressDumpParseException.unknownColumn(statementIndex, table, column);
            }
        }
    }
}
