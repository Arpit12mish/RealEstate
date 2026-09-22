package com.brandPitara.sfs.migration.wordpress.dump;

/**
 * Streaming callback for {@link WordPressDumpReader} - one method invoked per parsed row, in
 * source order, table by table. Every method defaults to a no-op so a caller only interested in,
 * say, posts and postmeta never has to implement the rest.
 */
public interface WordPressDumpVisitor {

    default void onPost(WordPressPostRow row) {
    }

    default void onPostMeta(WordPressPostMetaRow row) {
    }

    default void onTerm(WordPressTermRow row) {
    }

    default void onTermTaxonomy(WordPressTermTaxonomyRow row) {
    }

    default void onTermRelationship(WordPressTermRelationshipRow row) {
    }

    default void onTermMeta(WordPressTermMetaRow row) {
    }

    default void onUser(WordPressUserRow row) {
    }
}
