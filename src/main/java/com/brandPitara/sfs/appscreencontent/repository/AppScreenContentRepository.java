package com.brandPitara.sfs.appscreencontent.repository;

import com.brandPitara.sfs.appscreencontent.entity.AppScreenContentEntity;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AppScreenContentRepository extends JpaRepository<AppScreenContentEntity, Long> {

    List<AppScreenContentEntity> findByScreenKeyOrderByPlacementAscSortOrderAscIdDesc(AppScreenKey screenKey);

    List<AppScreenContentEntity> findByScreenKeyAndPlacementOrderBySortOrderAscIdDesc(
            AppScreenKey screenKey,
            AppScreenPlacement placement
    );

    @Query("""
            select c
            from AppScreenContentEntity c
            where c.screenKey = :screenKey
              and c.placement = :placement
              and c.enabled = true
              and (c.startAt is null or c.startAt <= :now)
              and (c.endAt is null or c.endAt >= :now)
            order by c.sortOrder asc, c.id desc
            """)
    List<AppScreenContentEntity> findActiveForPlacement(
            @Param("screenKey") AppScreenKey screenKey,
            @Param("placement") AppScreenPlacement placement,
            @Param("now") OffsetDateTime now
    );
}
