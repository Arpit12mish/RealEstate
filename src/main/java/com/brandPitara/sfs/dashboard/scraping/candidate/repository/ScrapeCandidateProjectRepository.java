package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScrapeCandidateProjectRepository
        extends JpaRepository<DashboardScrapeCandidateProjectEntity, Long> {

    Optional<DashboardScrapeCandidateProjectEntity> findByCandidateId(Long candidateId);

    List<DashboardScrapeCandidateProjectEntity> findByCandidateIdIn(Collection<Long> candidateIds);

    void deleteByCandidateId(Long candidateId);
}
