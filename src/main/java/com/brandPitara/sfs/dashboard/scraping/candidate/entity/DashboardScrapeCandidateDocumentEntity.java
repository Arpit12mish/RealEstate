package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateDocumentType;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "dashboard_scrape_candidate_document",
        indexes = @Index(name = "idx_scrape_cand_document_cid", columnList = "candidate_id")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateDocumentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private DashboardScrapeCandidateEntity candidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 80)
    private ScrapeCandidateDocumentType documentType;

    @Column(length = 255)
    private String title;

    @Column(name = "document_url", columnDefinition = "text")
    private String documentUrl;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;
}
