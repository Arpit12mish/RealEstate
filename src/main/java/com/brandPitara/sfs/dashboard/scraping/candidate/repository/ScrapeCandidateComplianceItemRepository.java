package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateComplianceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapeCandidateComplianceItemRepository
        extends JpaRepository<DashboardScrapeCandidateComplianceItemEntity, Long> {

    List<DashboardScrapeCandidateComplianceItemEntity> findByCandidateIdOrderByDisplayOrderAscIdAsc(Long candidateId);

    void deleteByCandidateId(Long candidateId);
}
