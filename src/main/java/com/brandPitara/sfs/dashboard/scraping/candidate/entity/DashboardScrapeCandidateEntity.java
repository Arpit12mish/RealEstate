package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldConfidenceStatus;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_scrape_candidate",
        indexes = {
                @Index(name = "idx_scrape_candidate_rera_number",    columnList = "rera_number"),
                @Index(name = "idx_scrape_candidate_source_code",    columnList = "source_code"),
                @Index(name = "idx_scrape_candidate_status",         columnList = "status"),
                @Index(name = "idx_scrape_candidate_linked_builder", columnList = "linked_builder_id"),
                @Index(name = "idx_scrape_candidate_linked_project", columnList = "linked_project_id")
        }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_code", nullable = false, length = 50)
    private ReraSourceCode sourceCode;

    @Column(name = "rera_number", nullable = false, length = 150)
    private String reraNumber;

    @Column(nullable = false)
    private boolean found;

    @Column(nullable = false)
    private boolean captchaDetected;

    @Column(name = "source_search_url", columnDefinition = "text")
    private String sourceSearchUrl;

    @Column(name = "source_detail_url", columnDefinition = "text")
    private String sourceDetailUrl;

    @Column(name = "final_url", columnDefinition = "text")
    private String finalUrl;

    @Column(name = "page_title", length = 300)
    private String pageTitle;

    @Column(name = "raw_html_path", columnDefinition = "text")
    private String rawHtmlPath;

    @Column(name = "screenshot_path", columnDefinition = "text")
    private String screenshotPath;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_status", length = 50)
    private ScrapeFieldConfidenceStatus confidenceStatus;

    @Column(name = "total_expected_fields")
    private Integer totalExpectedFields;

    @Column(name = "found_fields")
    private Integer foundFields;

    @Column(name = "missing_fields")
    private Integer missingFields;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ScrapeCandidateStatus status;

    @Column(name = "linked_builder_id")
    private Long linkedBuilderId;

    @Column(name = "linked_project_id")
    private Long linkedProjectId;

    @Column(name = "applied_project_id")
    private Long appliedProjectId;

    @Column(name = "created_by_dashboard_user_id")
    private Long createdByDashboardUserId;

    @Column(name = "applied_by_dashboard_user_id")
    private Long appliedByDashboardUserId;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    @Column(columnDefinition = "text")
    private String remarks;

    /** In-memory scrape session ID (kept here so refresh-captcha can look up the live session). */
    @Column(name = "captcha_session_id", length = 100)
    private String captchaSessionId;
}
