package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.FavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    boolean existsByUser_IdAndBusiness_Id(Long userId, Long businessId);

    Optional<FavoriteEntity> findByUser_IdAndBusiness_Id(Long userId, Long businessId);

    void deleteByUser_IdAndBusiness_Id(Long userId, Long businessId);

    @EntityGraph(attributePaths = {"business", "business.city", "business.category"})
    Page<FavoriteEntity> findByUser_Id(Long userId, Pageable pageable);

    // ✅ Total favorites for a business
    long countByBusiness_Id(Long businessId);

    // ✅ Batch: counts for many businesses in one query
    @Query("""
        select f.business.id as businessId, count(f.id) as cnt
        from FavoriteEntity f
        where f.business.id in :businessIds
        group by f.business.id
    """)
    List<Object[]> countByBusinessIds(List<Long> businessIds);

    // ✅ Batch: which of these businesses are favorited by current user
    @Query("""
        select f.business.id
        from FavoriteEntity f
        where f.user.id = :userId and f.business.id in :businessIds
    """)
    List<Long> findFavoritedBusinessIds(Long userId, List<Long> businessIds);
}
