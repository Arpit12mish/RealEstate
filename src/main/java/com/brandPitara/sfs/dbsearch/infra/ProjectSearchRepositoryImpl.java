package com.brandPitara.sfs.dbsearch.infra;

import com.brandPitara.sfs.dbsearch.app.SearchCriteria;
import com.brandPitara.sfs.dbsearch.app.SearchSort;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Locale;

@RequiredArgsConstructor
public class ProjectSearchRepositoryImpl implements ProjectSearchRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Page<ProjectEntity> searchProjects(SearchCriteria criteria, Pageable pageable) {
        StringBuilder where = new StringBuilder("""
                where p.active = true
                  and p.published = true
                  and p.deleted = false
                  and p.reviewStatus = com.brandPitara.sfs.dashboard.common.enums.ReviewStatus.APPROVED
                  and b.active = true
                  and b.published = true
                  and b.deleted = false
                  and (c is null or c.active = true)
                """);

        appendFilters(where, criteria);

        String contentJpql = """
                select p
                from ProjectEntity p
                join fetch p.builder b
                left join fetch p.city c
                left join ProjectMeterSnapshotEntity snapshot on snapshot.project = p
                """
                + where
                + orderBy(criteria);

        TypedQuery<ProjectEntity> contentQuery = entityManager.createQuery(contentJpql, ProjectEntity.class);
        applyParameters(contentQuery, criteria);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        String countJpql = """
                select count(p.id)
                from ProjectEntity p
                join p.builder b
                left join p.city c
                left join ProjectMeterSnapshotEntity snapshot on snapshot.project = p
                """
                + where;

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        applyParameters(countQuery, criteria);

        return new PageImpl<>(contentQuery.getResultList(), pageable, countQuery.getSingleResult());
    }

    private void appendFilters(StringBuilder where, SearchCriteria criteria) {
        if (hasKeyword(criteria)) {
            where.append("""
                  and (
                        lower(p.name) like :queryPrefix
                     or lower(p.name) like :queryContains
                     or lower(b.name) like :queryPrefix
                     or lower(b.name) like :queryContains
                     or lower(p.addressLine) like :queryContains
                     or lower(c.name) like :queryContains
                  )
                  and (:queryExact is null or :queryExact is not null)
                """);
        }

        if (criteria.cityId() != null) {
            where.append("  and c.id = :cityId\n");
        }

        if (criteria.priceMin() != null || criteria.priceMax() != null) {
            where.append("""
                  and coalesce(p.priceMin, p.priceMax) is not null
                """);
            if (criteria.priceMax() != null) {
                where.append("  and coalesce(p.priceMin, p.priceMax) <= :priceMax\n");
            }
            if (criteria.priceMin() != null) {
                where.append("  and coalesce(p.priceMax, p.priceMin) >= :priceMin\n");
            }
        }

        if (criteria.constructionProgressMin() != null || criteria.constructionProgressMax() != null) {
            where.append("""
                  and snapshot.constructionProgressPercent is not null
                """);
            if (criteria.constructionProgressMin() != null) {
                where.append("  and snapshot.constructionProgressPercent >= :constructionProgressMin\n");
            }
            if (criteria.constructionProgressMax() != null) {
                where.append("  and snapshot.constructionProgressPercent <= :constructionProgressMax\n");
            }
        }

        if (criteria.possessionFrom() != null || criteria.possessionTo() != null) {
            where.append("  and p.possessionDate is not null\n");
            if (criteria.possessionFrom() != null) {
                where.append("  and p.possessionDate >= :possessionFrom\n");
            }
            if (criteria.possessionTo() != null) {
                where.append("  and p.possessionDate <= :possessionTo\n");
            }
        }

        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            where.append("  and p.status in :statuses\n");
        }

        if (criteria.propertyTypes() != null && !criteria.propertyTypes().isEmpty()) {
            where.append("""
                  and exists (
                    select 1
                    from ProjectEntity p2
                    join p2.propertyTypes pt
                    where p2 = p
                      and pt in :propertyTypes
                  )
                """);
        }
    }

    private void applyParameters(Query query, SearchCriteria criteria) {
        if (hasKeyword(criteria)) {
            String normalized = criteria.query().trim().toLowerCase(Locale.ROOT);
            query.setParameter("queryExact", normalized);
            query.setParameter("queryPrefix", normalized + "%");
            query.setParameter("queryContains", "%" + normalized + "%");
        }
        if (criteria.cityId() != null) {
            query.setParameter("cityId", criteria.cityId());
        }
        if (criteria.priceMin() != null) {
            query.setParameter("priceMin", criteria.priceMin());
        }
        if (criteria.priceMax() != null) {
            query.setParameter("priceMax", criteria.priceMax());
        }
        if (criteria.constructionProgressMin() != null) {
            query.setParameter("constructionProgressMin", criteria.constructionProgressMin());
        }
        if (criteria.constructionProgressMax() != null) {
            query.setParameter("constructionProgressMax", criteria.constructionProgressMax());
        }
        if (criteria.possessionFrom() != null) {
            query.setParameter("possessionFrom", criteria.possessionFrom());
        }
        if (criteria.possessionTo() != null) {
            query.setParameter("possessionTo", criteria.possessionTo());
        }
        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            query.setParameter("statuses", criteria.statuses());
        }
        if (criteria.propertyTypes() != null && !criteria.propertyTypes().isEmpty()) {
            query.setParameter("propertyTypes", criteria.propertyTypes());
        }
    }

    private String orderBy(SearchCriteria criteria) {
        SearchSort sort = criteria.sort();

        if (sort == SearchSort.PRICE_LOW) {
            return """
                    order by case when p.priceMin is null then 1 else 0 end,
                             p.priceMin asc,
                             p.id desc
                    """;
        }
        if (sort == SearchSort.PRICE_HIGH) {
            return """
                    order by case when coalesce(p.priceMax, p.priceMin) is null then 1 else 0 end,
                             coalesce(p.priceMax, p.priceMin) desc,
                             p.id desc
                    """;
        }
        if (sort == SearchSort.POSSESSION_ASC) {
            return """
                    order by case when p.possessionDate is null then 1 else 0 end,
                             p.possessionDate asc,
                             p.id desc
                    """;
        }
        if (sort == SearchSort.PROGRESS_HIGH) {
            return """
                    order by case when snapshot.constructionProgressPercent is null then 1 else 0 end,
                             snapshot.constructionProgressPercent desc,
                             p.id desc
                    """;
        }
        if (sort == SearchSort.NEWEST) {
            return """
                    order by p.createdAt desc,
                             p.id desc
                    """;
        }
        if (sort == SearchSort.PRIORITY || !hasKeyword(criteria)) {
            return """
                    order by p.priority asc,
                             p.id desc
                    """;
        }

        return """
                order by case
                           when lower(p.name) = lower(:queryExact) then 1
                           when lower(p.name) like :queryPrefix then 2
                           when lower(b.name) = lower(:queryExact) then 3
                           when lower(b.name) like :queryPrefix then 4
                           when lower(p.name) like :queryContains then 5
                           when lower(b.name) like :queryContains then 6
                           else 7
                         end,
                         p.priority asc,
                         p.id desc
                """;
    }

    private boolean hasKeyword(SearchCriteria criteria) {
        return criteria != null
                && criteria.query() != null
                && !criteria.query().trim().isBlank();
    }
}
