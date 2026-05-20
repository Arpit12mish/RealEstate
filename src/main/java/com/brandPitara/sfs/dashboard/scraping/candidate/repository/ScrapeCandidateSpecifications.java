package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateEntity;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ScrapeCandidateSpecifications {

    private ScrapeCandidateSpecifications() {}

    public static Specification<DashboardScrapeCandidateEntity> filter(
            ScrapeCandidateStatus status,
            ReraSourceCode sourceCode,
            String reraNumber
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (sourceCode != null) {
                predicates.add(cb.equal(root.get("sourceCode"), sourceCode));
            }
            if (StringUtils.hasText(reraNumber)) {
                String pattern = "%" + reraNumber.trim().toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.upper(root.get("reraNumber")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
