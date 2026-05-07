package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<CityEntity, Long> {

    Optional<CityEntity> findByNameIgnoreCaseAndStateIgnoreCase(String name, String state);

    List<CityEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<CityEntity> findTop50ByOrderByNameAsc();

    @Query("""
        select c
        from CityEntity c
        where c.latitude is not null
          and c.longitude is not null
    """)
    List<CityEntity> findAllWithCoordinates();

    Optional<CityEntity> findFirstByNameIgnoreCase(String name);
}