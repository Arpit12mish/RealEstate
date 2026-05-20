package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapeCandidateDocumentRepository
        extends JpaRepository<DashboardScrapeCandidateDocumentEntity, Long> {

    List<DashboardScrapeCandidateDocumentEntity> findByCandidateIdOrderByDocumentTypeAscIdAsc(Long candidateId);

    void deleteByCandidateId(Long candidateId);
}
