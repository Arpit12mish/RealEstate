package com.brandPitara.sfs.dashboard.scraping.candidate.repository;

import com.brandPitara.sfs.dashboard.scraping.candidate.entity.DashboardScrapeCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScrapeCandidateRepository
        extends JpaRepository<DashboardScrapeCandidateEntity, Long>,
                JpaSpecificationExecutor<DashboardScrapeCandidateEntity> {
}
