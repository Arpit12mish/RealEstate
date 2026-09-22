package com.brandPitara.sfs.migration.wordpress.dump;

/**
 * The only two wp_users fields this reader ever retains. Deliberately narrow: password hashes,
 * activation keys, login names, emails, and every other authentication/session-adjacent column
 * are dropped by {@link WordPressDumpReader} at the moment a wp_users row is parsed - they never
 * exist in any object reachable from here, not merely "excluded from logging".
 */
public record WordPressUserRow(long id, String displayName) {
}
