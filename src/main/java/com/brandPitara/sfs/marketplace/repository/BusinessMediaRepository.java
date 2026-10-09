package com.brandPitara.sfs.marketplace.repository;

import com.brandPitara.sfs.marketplace.entity.BusinessMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BusinessMediaRepository extends JpaRepository<BusinessMediaEntity, Long> {

    @Query("""
            select m from BusinessMediaEntity m
            where m.business.id in :businessIds and m.deleted = false and m.active = true
            order by m.business.id asc, m.usageType asc, m.sortOrder asc, m.id asc
            """)
    List<BusinessMediaEntity> findPublicByBusinessIds(@Param("businessIds") Collection<Long> businessIds);

    List<BusinessMediaEntity> findByBusiness_IdAndDeletedFalseOrderByUsageTypeAscSortOrderAscIdAsc(Long businessId);

    Optional<BusinessMediaEntity> findByIdAndBusiness_IdAndDeletedFalse(Long id, Long businessId);

    long countByBusiness_IdAndDeletedFalse(Long businessId);
}
