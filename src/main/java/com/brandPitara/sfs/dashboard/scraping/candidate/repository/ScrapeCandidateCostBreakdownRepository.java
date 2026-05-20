package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateCostBreakdownEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScrapeCandidateCostBreakdownRepository
        extends JpaRepository<DashboardScrapeCandidateCostBreakdownEntity, Long> {

    Optional<DashboardScrapeCandidateCostBreakdownEntity> findByCandidateId(Long candidateId);

    void deleteByCandidateId(Long candidateId);
}
