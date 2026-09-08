package com.brandPitara.sfs.publiccontent.repository;

import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PublicContentRepository extends Repository<ContentPostEntity, Long> {

    @Query("""
            select post.id as postId,
                   revision.id as revisionId,
                   revision.contentType as contentType,
                   revision.title as title,
                   revision.slug as slug,
                   revision.excerpt as excerpt,
                   revision.readingTimeMinutes as readingTimeMinutes,
                   revision.publicAuthorId as publicAuthorId,
                   revision.publicAuthorName as publicAuthorName,
                   revision.publicAuthorSlug as publicAuthorSlug,
                   revision.publicAuthorDesignation as publicAuthorDesignation,
                   revision.publicAuthorProfileMediaAssetId as publicAuthorProfileMediaAssetId,
                   revision.categoryId as categoryId,
                   revision.categoryName as categoryName,
                   revision.categorySlug as categorySlug,
                   revision.tagSnapshots as tagSnapshots,
                   revision.coverMediaAssetId as coverMediaAssetId,
                   revision.coverAltText as coverAltText,
                   revision.seoTitle as seoTitle,
                   revision.seoDescription as seoDescription,
                   revision.canonicalUrl as canonicalUrl,
                   revision.robotsIndex as robotsIndex,
                   revision.robotsFollow as robotsFollow,
                   revision.contentDocument as contentDocument,
                   revision.contentDocumentSchemaVersion as contentDocumentSchemaVersion,
                   revision.createdAt as revisionCreatedAt,
                   post.publishedAt as publishedAt
            from ContentPostEntity post
            join post.currentPublishedRevision revision
            where post.status = com.brandPitara.sfs.cms.content.domain.ContentStatus.PUBLISHED
              and post.slug = :slug
              and revision.contentPost.id = post.id
            """)
    Optional<PublicContentDetailView> findPublishedDetailBySlug(@Param("slug") String slug);

    @Query(value = """
            select post.id as postId,
                   revision.contentType as contentType,
                   revision.title as title,
                   revision.slug as slug,
                   revision.excerpt as excerpt,
                   revision.readingTimeMinutes as readingTimeMinutes,
                   revision.publicAuthorId as publicAuthorId,
                   revision.publicAuthorName as publicAuthorName,
                   revision.publicAuthorSlug as publicAuthorSlug,
                   revision.publicAuthorDesignation as publicAuthorDesignation,
                   revision.categoryId as categoryId,
                   revision.categoryName as categoryName,
                   revision.categorySlug as categorySlug,
                   revision.coverMediaAssetId as coverMediaAssetId,
                   revision.coverAltText as coverAltText,
                   post.publishedAt as publishedAt
            from ContentPostEntity post
            join post.currentPublishedRevision revision
            where post.status = com.brandPitara.sfs.cms.content.domain.ContentStatus.PUBLISHED
              and revision.contentPost.id = post.id
              and (:contentType is null or revision.contentType = :contentType)
              and (:categorySlug is null or revision.categorySlug = :categorySlug)
              and (:authorSlug is null or revision.publicAuthorSlug = :authorSlug)
              and (:tagSlug is null or function('cms_revision_has_tag', revision.tagSnapshots, :tagSlug) = true)
              and (:searchTerm is null
                   or lower(revision.title) like :searchTerm escape '\\'
                   or lower(revision.excerpt) like :searchTerm escape '\\'
                   or lower(revision.slug) like :searchTerm escape '\\'
                   or lower(revision.categoryName) like :searchTerm escape '\\')
            order by post.publishedAt desc, post.id desc
            """, countQuery = """
            select count(post.id)
            from ContentPostEntity post
            join post.currentPublishedRevision revision
            where post.status = com.brandPitara.sfs.cms.content.domain.ContentStatus.PUBLISHED
              and revision.contentPost.id = post.id
              and (:contentType is null or revision.contentType = :contentType)
              and (:categorySlug is null or revision.categorySlug = :categorySlug)
              and (:authorSlug is null or revision.publicAuthorSlug = :authorSlug)
              and (:tagSlug is null or function('cms_revision_has_tag', revision.tagSnapshots, :tagSlug) = true)
              and (:searchTerm is null
                   or lower(revision.title) like :searchTerm escape '\\'
                   or lower(revision.excerpt) like :searchTerm escape '\\'
                   or lower(revision.slug) like :searchTerm escape '\\'
                   or lower(revision.categoryName) like :searchTerm escape '\\')
            """)
    Page<PublicContentListView> findPublishedListFiltered(
            @Param("contentType") ContentType contentType,
            @Param("categorySlug") String categorySlug,
            @Param("authorSlug") String authorSlug,
            @Param("tagSlug") String tagSlug,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    default Page<PublicContentListView> findPublishedList(ContentType contentType, Pageable pageable) {
        return findPublishedListFiltered(contentType, null, null, null, null, pageable);
    }
}
