package com.brandPitara.sfs.request.repository;

import com.brandPitara.sfs.request.entity.ServiceRequestEntity;
import com.brandPitara.sfs.request.enums.ServiceRequestStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
public interface ServiceRequestRepository extends JpaRepository<ServiceRequestEntity, Long> {

    Page<ServiceRequestEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Page<ServiceRequestEntity> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            Long customerId, ServiceRequestStatus status, Pageable pageable
    );

    /**
     * Provider feed:
     * - status filter
     * - request city in provider cityIds
     * - (optional) pincode in provider pincodes
     * - provider primary category must be present in request.categories
     *
     * Note: pincodes can be empty -> ignore pincode filter.
     */
    @Query(
        value = """
            select distinct r
            from ServiceRequestEntity r
            join r.categories c
            where r.status = :status
              and r.city.id in :cityIds
              and (:ignorePincode = true or r.pincode in :pincodes)
              and c.id = :categoryId
            order by r.createdAt desc
        """,
        countQuery = """
            select count(distinct r.id)
            from ServiceRequestEntity r
            join r.categories c
            where r.status = :status
              and r.city.id in :cityIds
              and (:ignorePincode = true or r.pincode in :pincodes)
              and c.id = :categoryId
        """
    )
    Page<ServiceRequestEntity> findProviderFeed(
            @Param("status") ServiceRequestStatus status,
            @Param("cityIds") Set<Long> cityIds,
            @Param("ignorePincode") boolean ignorePincode,
            @Param("pincodes") Set<String> pincodes,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("""
        select count(distinct r.id)
        from ServiceRequestEntity r
        join r.categories c
        where r.status = :status
        and r.city.id in :cityIds
        and (:ignorePincode = true or r.pincode in :pincodes)
        and c.id = :categoryId
    """)
    long countProviderFeed(
            @Param("status") ServiceRequestStatus status,
            @Param("cityIds") Set<Long> cityIds,
            @Param("ignorePincode") boolean ignorePincode,
            @Param("pincodes") Set<String> pincodes,
            @Param("categoryId") Long categoryId
    );

    @Query("""
        select distinct r
        from ServiceRequestEntity r
        join r.categories c
        where r.status = :status
        and r.city.id in :cityIds
        and (:ignorePincode = true or r.pincode in :pincodes)
        and c.id = :categoryId
        order by r.createdAt desc
    """)
    List<ServiceRequestEntity> findTopProviderFeed(
            @Param("status") ServiceRequestStatus status,
            @Param("cityIds") Set<Long> cityIds,
            @Param("ignorePincode") boolean ignorePincode,
            @Param("pincodes") Set<String> pincodes,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}
