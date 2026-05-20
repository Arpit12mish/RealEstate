package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "dashboard_scrape_candidate_field_result",
        indexes = @Index(name = "idx_scrape_cand_field_result_cid", columnList = "candidate_id")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateFieldResultEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private DashboardScrapeCandidateEntity candidate;

    @Column(length = 100)
    private String section;

    @Column(name = "field_key", length = 150)
    private String fieldKey;

    @Column(name = "field_label", length = 200)
    private String fieldLabel;

    @Column(nullable = false)
    private boolean found;

    @Column(name = "value_text", columnDefinition = "text")
    private String valueText;

    @Column(name = "source_label", length = 255)
    private String sourceLabel;

    private Integer confidence;

    @Column(columnDefinition = "text")
    private String reason;
}
