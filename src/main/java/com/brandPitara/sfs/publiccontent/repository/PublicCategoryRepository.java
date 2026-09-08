package com.brandPitara.sfs.publiccontent.repository;

import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface PublicCategoryRepository extends Repository<CmsContentCategoryEntity, Long> {

    /**
     * One aggregate query - the published-content count is a correlated
     * subquery evaluated by Postgres per category row inside a single
     * statement, not N+1 round trips from the application. Counts against
     * the same revision.categoryId that PublicContentRepository's list/detail
     * queries read from (the immutable snapshot on the published revision),
     * so a category's count always agrees with what ?categorySlug= actually
     * returns - not the live, possibly-since-changed post.category FK.
     */
    @Query("""
            select c.id as id, c.name as name, c.slug as slug, c.description as description,
                   (select count(post.id) from ContentPostEntity post
                     where post.status = com.brandPitara.sfs.cms.content.domain.ContentStatus.PUBLISHED
                       and post.currentPublishedRevision.categoryId = c.id) as publishedContentCount
            from CmsContentCategoryEntity c
            where c.active = true
            order by c.name asc, c.id asc
            """)
    List<PublicCategorySummaryView> findActiveCategorySummaries();
}
