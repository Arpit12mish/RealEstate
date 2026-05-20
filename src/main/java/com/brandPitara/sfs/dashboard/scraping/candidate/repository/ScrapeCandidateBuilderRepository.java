package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateBuilderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScrapeCandidateBuilderRepository
        extends JpaRepository<DashboardScrapeCandidateBuilderEntity, Long> {

    Optional<DashboardScrapeCandidateBuilderEntity> findByCandidateId(Long candidateId);

    List<DashboardScrapeCandidateBuilderEntity> findByCandidateIdIn(Collection<Long> candidateIds);

    void deleteByCandidateId(Long candidateId);
}
