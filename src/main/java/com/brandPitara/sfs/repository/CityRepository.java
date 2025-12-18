package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<CityEntity, Long> {

    Optional<CityEntity> findByNameIgnoreCaseAndStateIgnoreCase(String name, String state);

    // search by name
    List<CityEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    // default list if no search text
    List<CityEntity> findTop50ByOrderByNameAsc();
}
