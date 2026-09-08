package com.brandPitara.sfs.cms.taxonomy.repository;

import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CmsContentCategoryRepository extends JpaRepository<CmsContentCategoryEntity, Long> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    @Query("select c from CmsContentCategoryEntity c where (:active is null or c.active=:active) and (:search is null or lower(c.name) like :search or lower(c.slug) like :search)")
    Page<CmsContentCategoryEntity> search(@Param("active") Boolean active, @Param("search") String search, Pageable pageable);
}
