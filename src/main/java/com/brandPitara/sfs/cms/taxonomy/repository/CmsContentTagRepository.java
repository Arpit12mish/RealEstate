package com.brandPitara.sfs.cms.taxonomy.repository;

import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentTagEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CmsContentTagRepository extends JpaRepository<CmsContentTagEntity, Long> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    @Query("select t from CmsContentTagEntity t where (:active is null or t.active=:active) and (:search is null or lower(t.name) like :search or lower(t.slug) like :search)")
    Page<CmsContentTagEntity> search(@Param("active") Boolean active, @Param("search") String search, Pageable pageable);
}
