package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateFieldResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapeCandidateFieldResultRepository
        extends JpaRepository<DashboardScrapeCandidateFieldResultEntity, Long> {

    List<DashboardScrapeCandidateFieldResultEntity> findByCandidateIdOrderByFoundDescFieldKeyAsc(Long candidateId);

    void deleteByCandidateId(Long candidateId);
}
