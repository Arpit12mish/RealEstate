package com.brandPitara.sfs.migration.wordpress.audit;

import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Computes {@link MediaReferenceReconciliation} over a fixed set of posts against a {@link WordPressDataset}. */
public final class WordPressMediaReferenceAnalyzer {

    private static final Pattern IMG_SRC = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SIZE_SUFFIX = Pattern.compile("-\\d+x\\d+(\\.\\w+)$");
    private static final String OWN_DOMAIN = "squarefootstory.com";

    public MediaReferenceReconciliation analyze(Collection<WordPressPostRow> targetPosts, WordPressDataset dataset) {
        List<String> rawOccurrences = new ArrayList<>();
        for (WordPressPostRow post : targetPosts) {
            Matcher matcher = IMG_SRC.matcher(post.postContent() == null ? "" : post.postContent());
            while (matcher.find()) {
                rawOccurrences.add(matcher.group(1));
            }
        }

        Set<String> uniqueOriginal = new LinkedHashSet<>(rawOccurrences);

        Map<String, String> normalizedByOriginal = new LinkedHashMap<>();
        for (String url : uniqueOriginal) {
            normalizedByOriginal.put(url, normalize(url));
        }
        Set<String> normalizedUnique = new LinkedHashSet<>(normalizedByOriginal.values());

        Set<String> ownDomain = new LinkedHashSet<>();
        Set<String> external = new LinkedHashSet<>();
        for (String url : normalizedUnique) {
            if (isOwnDomain(url)) {
                ownDomain.add(url);
            } else {
                external.add(url);
            }
        }

        Set<String> attachedFiles = attachedFilePaths(dataset);
        int attachmentBacked = 0;
        int unresolved = 0;
        for (String url : ownDomain) {
            String relativePath = relativeUploadPath(url);
            if (relativePath != null && attachedFiles.contains(relativePath)) {
                attachmentBacked++;
            } else {
                unresolved++;
            }
        }

        return new MediaReferenceReconciliation(
                rawOccurrences.size(), uniqueOriginal.size(), normalizedUnique.size(),
                ownDomain.size(), external.size(), attachmentBacked, unresolved
        );
    }

    private Set<String> attachedFilePaths(WordPressDataset dataset) {
        Set<String> paths = new LinkedHashSet<>();
        for (WordPressPostRow post : dataset.postsById().values()) {
            if (!"attachment".equals(post.postType())) {
                continue;
            }
            String attachedFile = dataset.metaValue(post.id(), "_wp_attached_file");
            if (attachedFile != null) {
                paths.add(normalize(attachedFile));
            }
        }
        return paths;
    }

    private String normalize(String url) {
        Matcher matcher = SIZE_SUFFIX.matcher(url);
        return matcher.find() ? matcher.replaceFirst(matcher.group(1)) : url;
    }

    private boolean isOwnDomain(String url) {
        return url.contains(OWN_DOMAIN) || url.startsWith("/");
    }

    private String relativeUploadPath(String url) {
        int marker = url.indexOf("/wp-content/uploads/");
        if (marker == -1) {
            return null;
        }
        return url.substring(marker + "/wp-content/uploads/".length());
    }
}
