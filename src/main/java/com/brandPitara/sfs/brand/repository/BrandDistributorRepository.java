package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandDistributorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrandDistributorRepository extends JpaRepository<BrandDistributorEntity, Long> {

  Optional<BrandDistributorEntity> findByBrandIdAndDistributorIdAndDeletedFalse(Long brandId, Long distributorId);

  Page<BrandDistributorEntity> findByBrandIdAndDeletedFalse(Long brandId, Pageable pageable);

  Page<BrandDistributorEntity> findByBrandIdAndActiveTrueAndDeletedFalseOrderByPriorityAsc(Long brandId, Pageable pageable);

  List<BrandDistributorEntity> findByDistributorIdAndActiveTrueAndDeletedFalse(Long distributorId);

    @Query("""
    select bd
    from BrandDistributorEntity bd
      join fetch bd.brand b
      join fetch bd.distributor d
    where d.id = :distributorId
      and bd.deleted = false
      and bd.active = true
      and b.deleted = false
      and b.active = true
      and b.published = true
      and d.deleted = false
      and d.active = true
    order by bd.priority asc, b.priority asc, b.name asc
  """)
  List<BrandDistributorEntity> findPublicBrandMappingsForDistributor(@Param("distributorId") Long distributorId);
}
