package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateRawValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapeCandidateRawValueRepository
        extends JpaRepository<DashboardScrapeCandidateRawValueEntity, Long> {

    List<DashboardScrapeCandidateRawValueEntity> findByCandidateIdOrderByRawKeyAsc(Long candidateId);

    void deleteByCandidateId(Long candidateId);
}
