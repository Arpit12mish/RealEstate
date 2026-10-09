package com.brandPitara.sfs.marketplace.repository;

import com.brandPitara.sfs.marketplace.entity.BusinessOpeningHoursEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BusinessOpeningHoursRepository extends JpaRepository<BusinessOpeningHoursEntity, Long> {

    @Query("""
            select h from BusinessOpeningHoursEntity h
            where h.business.id in :businessIds
            order by h.business.id asc, h.dayOfWeek asc, h.opensAt asc
            """)
    List<BusinessOpeningHoursEntity> findByBusinessIds(@Param("businessIds") Collection<Long> businessIds);

    @Modifying
    @Query("delete from BusinessOpeningHoursEntity h where h.business.id = :businessId")
    void deleteByBusinessId(@Param("businessId") Long businessId);
}
