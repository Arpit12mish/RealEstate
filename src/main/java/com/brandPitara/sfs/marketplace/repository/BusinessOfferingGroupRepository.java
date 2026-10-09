package com.brandPitara.sfs.marketplace.repository;

import com.brandPitara.sfs.marketplace.entity.BusinessOfferingGroupEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BusinessOfferingGroupRepository extends JpaRepository<BusinessOfferingGroupEntity, Long> {

    @Query("""
            select distinct g from BusinessOfferingGroupEntity g
            left join fetch g.items
            where g.business.id = :businessId
            order by g.offeringType asc, g.sortOrder asc, g.id asc
            """)
    List<BusinessOfferingGroupEntity> findWithItemsByBusinessId(@Param("businessId") Long businessId);

    /** (businessId, itemName) rows for product chips, ordered so the first N per business are the top ones. */
    @Query("""
            select g.business.id, i.name from BusinessOfferingItemEntity i
            join i.group g
            where g.business.id in :businessIds and g.offeringType = :type
            order by g.business.id asc, g.sortOrder asc, g.id asc, i.sortOrder asc, i.id asc
            """)
    List<Object[]> findItemNamesByBusinessIds(
            @Param("businessIds") Collection<Long> businessIds,
            @Param("type") BusinessOfferingType type
    );

    List<BusinessOfferingGroupEntity> findByBusiness_IdAndOfferingType(Long businessId, BusinessOfferingType type);

    @Modifying
    @Query("delete from BusinessOfferingGroupEntity g where g.business.id = :businessId and g.offeringType = :type")
    void deleteByBusinessIdAndType(@Param("businessId") Long businessId, @Param("type") BusinessOfferingType type);
}
