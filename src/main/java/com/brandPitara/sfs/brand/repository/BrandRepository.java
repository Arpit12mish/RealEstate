package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.builder.entity.BuilderEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

  Optional<BrandEntity> findByIdAndDeletedFalse(Long id);

  Optional<BrandEntity> findBySlug(String slug);

  Optional<BrandEntity> findBySlugAndIdNot(String slug, Long id);

  Optional<BrandEntity> findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long id);

  Optional<BrandEntity> findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse(String slug);

  Page<BrandEntity> findByDeletedFalse(Pageable pageable);

  Page<BrandEntity> findByPublishedAndDeletedFalse(boolean published, Pageable pageable);

  Page<BrandEntity> findByActiveAndDeletedFalse(boolean active, Pageable pageable);

  Page<BrandEntity> findByPublishedAndActiveAndDeletedFalse(boolean published, boolean active, Pageable pageable);

  Page<BrandEntity> findByPublishedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);
  List<BrandEntity> findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(Collection<Long> ids);

  Page<BrandEntity> findByCategory_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(
      Long categoryId,
      Pageable pageable
  );

  // Public brand discovery, no text search - optional category filter only. Used whenever
  // q is blank; kept as a fully separate query (rather than an "OR :q is null" branch) so a
  // null q is never bound into the same prepared statement as a lower(:q) call - see
  // searchPublicBrands below for why that matters.
  @Query(
      value = """
          select b
          from BrandEntity b
          where b.published = true and b.active = true and b.deleted = false
            and (:categoryId is null or exists (
                  select 1 from BrandCategoryLinkEntity l
                  where l.brand = b and l.category.id = :categoryId
                    and l.active = true and l.deleted = false
                ))
          """,
      countQuery = """
          select count(b)
          from BrandEntity b
          where b.published = true and b.active = true and b.deleted = false
            and (:categoryId is null or exists (
                  select 1 from BrandCategoryLinkEntity l
                  where l.brand = b and l.category.id = :categoryId
                    and l.active = true and l.deleted = false
                ))
          """
  )
  Page<BrandEntity> findPublicBrands(
      @Param("categoryId") Long categoryId,
      Pageable pageable
  );

  // Public brand discovery with a name/shortDescription search. Callers must only invoke
  // this with a non-blank q (see BrandPublicServiceImpl.listPublicBrands) - PostgreSQL
  // cannot infer a type for a null parameter bound only inside lower(...)/concat(...) and
  // fails with "function lower(bytea) does not exist" if q is ever actually null here.
  @Query(
      value = """
          select b
          from BrandEntity b
          where b.published = true and b.active = true and b.deleted = false
            and (:categoryId is null or exists (
                  select 1 from BrandCategoryLinkEntity l
                  where l.brand = b and l.category.id = :categoryId
                    and l.active = true and l.deleted = false
                ))
            and (lower(b.name) like lower(concat('%', :q, '%'))
              or lower(b.shortDescription) like lower(concat('%', :q, '%')))
          """,
      countQuery = """
          select count(b)
          from BrandEntity b
          where b.published = true and b.active = true and b.deleted = false
            and (:categoryId is null or exists (
                  select 1 from BrandCategoryLinkEntity l
                  where l.brand = b and l.category.id = :categoryId
                    and l.active = true and l.deleted = false
                ))
            and (lower(b.name) like lower(concat('%', :q, '%'))
              or lower(b.shortDescription) like lower(concat('%', :q, '%')))
          """
  )
  Page<BrandEntity> searchPublicBrands(
      @Param("categoryId") Long categoryId,
      @Param("q") String q,
      Pageable pageable
  );

  // Brands sharing at least one category with the given set, excluding the current brand,
  // public-visible only. Pageable is used purely to cap the result size (no total count needed).
  @Query("""
      select distinct b
      from BrandEntity b
        join BrandCategoryLinkEntity l on l.brand = b
      where l.category.id in :categoryIds
        and l.active = true and l.deleted = false
        and b.id <> :excludeBrandId
        and b.published = true and b.active = true and b.deleted = false
      order by b.priority asc, b.id desc
      """)
  List<BrandEntity> findRelatedBrands(
      @Param("categoryIds") Collection<Long> categoryIds,
      @Param("excludeBrandId") Long excludeBrandId,
      Pageable pageable
  );
}
