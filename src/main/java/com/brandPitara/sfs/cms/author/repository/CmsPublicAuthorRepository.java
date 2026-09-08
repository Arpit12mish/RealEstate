package com.brandPitara.sfs.cms.author.repository;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface CmsPublicAuthorRepository extends JpaRepository<CmsPublicAuthorEntity, Long> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    @EntityGraph(attributePaths = "profileMediaAsset")
    Optional<CmsPublicAuthorEntity> findWithProfileMediaById(Long id);
    @EntityGraph(attributePaths = "profileMediaAsset")
    @Query("""
        select a from CmsPublicAuthorEntity a
        where (:active is null or a.active = :active)
          and (:search is null or lower(a.displayName) like :search or lower(a.slug) like :search)
        """)
    Page<CmsPublicAuthorEntity> search(@Param("active") Boolean active,
                                      @Param("search") String search, Pageable pageable);
}
