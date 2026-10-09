package com.brandPitara.sfs.marketplace.repository;

import com.brandPitara.sfs.entity.BusinessEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

/**
 * Dealer reads over {@code business}. A dealer is an active business that is not a WORKER
 * provider's own linked listing - that rule lives only in these queries.
 */
public interface DealerRepository extends JpaRepository<BusinessEntity, Long> {

    // Leading space: text blocks strip the trailing space of the "and " they are appended to.
    String NOT_WORKER_LISTING = " " + """
            not exists (
                select 1 from ProviderProfileEntity wp
                where wp.business = b
                  and wp.providerType = com.brandPitara.sfs.provider.enums.ProviderType.WORKER
            )
            """;

    @Query("""
            select b from BusinessEntity b
            join fetch b.city
            join fetch b.category
            where b.id = :id and b.active = true and
            """ + NOT_WORKER_LISTING)
    Optional<BusinessEntity> findPublicDealer(@Param("id") Long id);

    /** Any dealer row regardless of activity, for dashboard management. */
    @Query("""
            select b from BusinessEntity b
            join fetch b.city
            join fetch b.category
            where b.id = :id and
            """ + NOT_WORKER_LISTING)
    Optional<BusinessEntity> findDealerForManagement(@Param("id") Long id);

    @Query("""
            select (count(b) > 0) from BusinessEntity b
            where b.id = :id and b.active = true and
            """ + NOT_WORKER_LISTING)
    boolean existsPublicDealer(@Param("id") Long id);

    /**
     * Same city, same or sibling category; the exact category ranks first, then rating, then id
     * so pagination is stable.
     */
    @Query(value = """
            select b from BusinessEntity b
            join fetch b.city
            join fetch b.category
            where b.id <> :excludeId
              and b.active = true
              and b.city.id = :cityId
              and b.category.id in :categoryIds
              and """ + NOT_WORKER_LISTING + """
            order by case when b.category.id = :categoryId then 0 else 1 end,
                     b.avgRating desc, b.totalRatings desc, b.id asc
            """,
            countQuery = """
            select count(b) from BusinessEntity b
            where b.id <> :excludeId
              and b.active = true
              and b.city.id = :cityId
              and b.category.id in :categoryIds
              and """ + NOT_WORKER_LISTING)
    Page<BusinessEntity> findSimilarDealers(
            @Param("excludeId") Long excludeId,
            @Param("cityId") Long cityId,
            @Param("categoryId") Long categoryId,
            @Param("categoryIds") Collection<Long> categoryIds,
            Pageable pageable
    );

    @Query(value = """
            select b from BusinessEntity b
            join fetch b.city
            join fetch b.category
            where b.active = true
              and (:cityId is null or b.city.id = :cityId)
              and (:categoryId is null or b.category.id = :categoryId or b.category.parent.id = :categoryId)
              and """ + NOT_WORKER_LISTING + """
            order by b.sponsored desc, b.sponsoredPriority desc, b.avgRating desc, b.id asc
            """,
            countQuery = """
            select count(b) from BusinessEntity b
            where b.active = true
              and (:cityId is null or b.city.id = :cityId)
              and (:categoryId is null or b.category.id = :categoryId or b.category.parent.id = :categoryId)
              and """ + NOT_WORKER_LISTING)
    Page<BusinessEntity> findDealers(
            @Param("cityId") Long cityId,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query(value = """
            select b from BusinessEntity b
            join fetch b.city
            join fetch b.category
            where (:q = '' or lower(b.name) like lower(concat('%', :q, '%')))
              and """ + NOT_WORKER_LISTING + """
            order by b.id desc
            """,
            countQuery = """
            select count(b) from BusinessEntity b
            where (:q = '' or lower(b.name) like lower(concat('%', :q, '%')))
              and """ + NOT_WORKER_LISTING)
    Page<BusinessEntity> searchDealersForManagement(@Param("q") String q, Pageable pageable);

    /** Row lock so concurrent moderations recompute rating aggregates serially. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BusinessEntity b where b.id = :id")
    Optional<BusinessEntity> lockById(@Param("id") Long id);
}
