package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "dashboard_scrape_candidate_raw_value",
        indexes = @Index(name = "idx_scrape_cand_raw_value_cid", columnList = "candidate_id")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateRawValueEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private DashboardScrapeCandidateEntity candidate;

    @Column(name = "raw_key", nullable = false, columnDefinition = "text")
    private String rawKey;

    @Column(name = "raw_value", columnDefinition = "text")
    private String rawValue;
}
