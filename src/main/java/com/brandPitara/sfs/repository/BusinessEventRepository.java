package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.BusinessEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface BusinessEventRepository extends JpaRepository<BusinessEventEntity, Long> {

    @Query("""
           SELECT e.eventType, COUNT(e)
           FROM BusinessEventEntity e
           WHERE e.business.id = :businessId
             AND e.createdAt >= :from
           GROUP BY e.eventType
           """)
    List<Object[]> countByTypeSince(Long businessId, OffsetDateTime from);
}
