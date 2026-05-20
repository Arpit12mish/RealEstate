package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateLandUtilizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScrapeCandidateLandUtilizationRepository
        extends JpaRepository<DashboardScrapeCandidateLandUtilizationEntity, Long> {

    Optional<DashboardScrapeCandidateLandUtilizationEntity> findByCandidateId(Long candidateId);

    void deleteByCandidateId(Long candidateId);
}
