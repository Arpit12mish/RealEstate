package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.entity.CityEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<CityEntity, Long> {

    Optional<CityEntity> findByNameIgnoreCaseAndStateIgnoreCase(String name, String state);

    Optional<CityEntity> findBySlugIgnoreCase(String slug);

    Optional<CityEntity> findBySlugIgnoreCaseAndIdNot(String slug, Long id);

    List<CityEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<CityEntity> findTop50ByOrderByNameAsc();

    @Query("""
        select c
        from CityEntity c
        where c.latitude is not null
          and c.longitude is not null
          and c.active = true
    """)
    List<CityEntity> findAllWithCoordinates();

    Optional<CityEntity> findFirstByNameIgnoreCase(String name);

    Optional<CityEntity> findFirstByNameIgnoreCaseAndActiveTrue(String name);

    @Query("""
        select new com.brandPitara.sfs.dto.TrendingCityCardResponse(
            c.id,
            c.name,
            c.slug,
            c.state,
            c.countryCode,
            c.coverImageUrl,
            count(p.id),
            c.growthPercent,
            c.displayOrder,
            case when count(p.id) = 0 then true else false end
        )
        from CityEntity c
        left join ProjectEntity p
          on p.city = c
         and p.published = true
         and p.active = true
         and p.deleted = false
         and p.reviewStatus = :reviewStatus
        where c.active = true
          and c.homepageFeatured = true
        group by
            c.id,
            c.name,
            c.slug,
            c.state,
            c.countryCode,
            c.coverImageUrl,
            c.growthPercent,
            c.displayOrder
        order by c.displayOrder asc, c.name asc
    """)
    List<TrendingCityCardResponse> findTrendingCityCards(
            @Param("reviewStatus") ReviewStatus reviewStatus,
            Pageable pageable
    );
}
