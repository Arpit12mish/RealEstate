package com.brandPitara.sfs.migration.wordpress.dump;

public record WordPressPostRow(
        long id,
        Long postAuthor,
        String postDate,
        String postDateGmt,
        String postModifiedGmt,
        String postContent,
        String postTitle,
        String postExcerpt,
        String postStatus,
        String postName,
        Long postParent,
        String postType,
        String postMimeType,
        String guid
) {
}
