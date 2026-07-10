package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrandCollaborationRepository extends JpaRepository<BrandCollaborationEntity, Long> {

  List<BrandCollaborationEntity> findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long brandId);

  List<BrandCollaborationEntity> findByProject_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<BrandCollaborationEntity> findByBuilder_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long builderId);

  List<BrandCollaborationEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long companyId);

  List<BrandCollaborationEntity> findByBusiness_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long businessId);

  Optional<BrandCollaborationEntity> findByIdAndBrand_IdAndDeletedFalse(Long id, Long brandId);

  boolean existsByBrand_IdAndProject_IdAndDeletedFalse(Long brandId, Long projectId);
  boolean existsByBrand_IdAndBuilder_IdAndDeletedFalse(Long brandId, Long builderId);
  boolean existsByBrand_IdAndCompany_IdAndDeletedFalse(Long brandId, Long companyId);
  boolean existsByBrand_IdAndBusiness_IdAndDeletedFalse(Long brandId, Long businessId);
  boolean existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(Long brandId, Long companyProjectId);

  boolean existsByBrand_IdAndProject_IdAndDeletedFalseAndIdNot(Long brandId, Long projectId, Long id);
  boolean existsByBrand_IdAndBuilder_IdAndDeletedFalseAndIdNot(Long brandId, Long builderId, Long id);
  boolean existsByBrand_IdAndCompany_IdAndDeletedFalseAndIdNot(Long brandId, Long companyId, Long id);
  boolean existsByBrand_IdAndBusiness_IdAndDeletedFalseAndIdNot(Long brandId, Long businessId, Long id);
  boolean existsByBrand_IdAndCompanyProject_IdAndDeletedFalseAndIdNot(Long brandId, Long companyProjectId, Long id);

  // Single query with all four target associations left-joined so an admin list page
  // never triggers per-row lazy loads regardless of which target type each row has.
  // Explicit countQuery: Spring Data's auto-derived count query would strip "fetch" from
  // the same JPQL, but a plain unqualified count(c) against the joined query risks
  // inflated/incorrect totals and is fragile with multiple left joins - a dedicated,
  // join-free count query is the correct and reliable way to paginate this.
  @Query(
      value = """
          select c
          from BrandCollaborationEntity c
            left join fetch c.project
            left join fetch c.builder
            left join fetch c.company
            left join fetch c.business
            left join fetch c.companyProject
          where c.brand.id = :brandId
            and c.deleted = false
          """,
      countQuery = """
          select count(c)
          from BrandCollaborationEntity c
          where c.brand.id = :brandId
            and c.deleted = false
          """
  )
  Page<BrandCollaborationEntity> findAdminPageByBrandId(@Param("brandId") Long brandId, Pageable pageable);

  // ---------- public read paths ----------

  // Batch collaboration stats for a page of brands: one aggregated row per distinct
  // (brand, targetType, companyType) combination as [brandId, targetType, companyType-or-null,
  // count]. Classification of COMPANY rows into architects/designers happens in the service
  // layer (BrandCompanyClassifier) - the count is pre-aggregated in the DB so this is a
  // single query regardless of how many collaborations exist, avoiding one query per brand row.
  @Query("""
      select c.brand.id, c.targetType, co.companyType, count(c)
      from BrandCollaborationEntity c
        left join c.company co
      where c.brand.id in :brandIds
        and c.active = true and c.deleted = false and c.publicVisible = true
        and c.targetType <> com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType.COMPANY_PROJECT
      group by c.brand.id, c.targetType, co.companyType
      """)
  List<Object[]> findPublicStatRowsByBrandIds(@Param("brandIds") Collection<Long> brandIds);

  // Brand detail page "topProjects" - only public-visible collaborations whose project is
  // itself public-visible (published/active/not-deleted/APPROVED).
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.project p
      where c.brand.id = :brandId
        and c.targetType = com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType.PROJECT
        and c.active = true and c.deleted = false and c.publicVisible = true
        and p.published = true and p.active = true and p.deleted = false
        and p.reviewStatus = com.brandPitara.sfs.dashboard.common.enums.ReviewStatus.APPROVED
      order by c.featured desc, c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findPublicProjectCollaborationsByBrandId(@Param("brandId") Long brandId);

  // Brand detail page "topBuilders".
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.builder bu
      where c.brand.id = :brandId
        and c.targetType = com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType.BUILDER
        and c.active = true and c.deleted = false and c.publicVisible = true
        and bu.published = true and bu.active = true and bu.deleted = false
      order by c.featured desc, c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findPublicBuilderCollaborationsByBrandId(@Param("brandId") Long brandId);

  // Brand detail page "topArchitects"/"interiorDesigners" (split by companyType in the
  // service layer via BrandCompanyClassifier).
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.company co
      where c.brand.id = :brandId
        and c.targetType = com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType.COMPANY
        and c.active = true and c.deleted = false and c.publicVisible = true
        and co.published = true and co.active = true and co.deleted = false
      order by c.featured desc, c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findPublicCompanyCollaborationsByBrandId(@Param("brandId") Long brandId);

  // Project detail page "connected brands": only public-visible collaborations whose
  // brand is itself public-visible. Pageable caps the result (no count query needed).
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.brand b
      where c.project.id = :projectId
        and c.active = true and c.deleted = false and c.publicVisible = true
        and b.published = true and b.active = true and b.deleted = false
      order by c.featured desc, c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findPublicByProjectId(@Param("projectId") Long projectId, Pageable pageable);

  // Builder detail page "connected brands" - same shape as the project variant.
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.brand b
      where c.builder.id = :builderId
        and c.active = true and c.deleted = false and c.publicVisible = true
        and b.published = true and b.active = true and b.deleted = false
      order by c.featured desc, c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findPublicByBuilderId(@Param("builderId") Long builderId, Pageable pageable);

  // Company profile "connected brands" from the canonical collaboration model. Legacy
  // company_brand_link fallback is handled in CompanyConnectedBrandPublicServiceImpl.
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.brand b
      where c.company.id = :companyId
        and c.targetType = com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType.COMPANY
        and c.active = true and c.deleted = false and c.publicVisible = true
        and b.published = true and b.active = true and b.deleted = false
      order by c.featured desc, c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findPublicByCompanyId(@Param("companyId") Long companyId);

  // Company portfolio project detail "Brands Used".
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.brand b
      where c.companyProject.id = :companyProjectId
        and c.targetType = com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType.COMPANY_PROJECT
        and c.active = true and c.deleted = false and c.publicVisible = true
        and b.published = true and b.active = true and b.deleted = false
      order by c.featured desc, c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findPublicByCompanyProjectId(
      @Param("companyProjectId") Long companyProjectId,
      Pageable pageable
  );

  // ---------- dashboard admin read/write paths (Company Project Brands Used) ----------

  // Admin view of a company project's brands used - includes rows the admin has deactivated
  // (active = false) so they remain visible/toggleable, unlike the public-only query above
  // which only ever returns active + public-visible rows.
  @Query("""
      select c
      from BrandCollaborationEntity c
        join fetch c.brand b
      where c.companyProject.id = :companyProjectId
        and c.targetType = com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType.COMPANY_PROJECT
        and c.deleted = false
      order by c.sortOrder asc, c.id asc
      """)
  List<BrandCollaborationEntity> findByCompanyProject_IdAndTargetTypeAndDeletedFalseOrderBySortOrderAscIdAsc(
      @Param("companyProjectId") Long companyProjectId,
      BrandCollaborationTargetType targetType
  );

  Optional<BrandCollaborationEntity> findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(
      Long id,
      Long companyProjectId,
      BrandCollaborationTargetType targetType
  );
}
