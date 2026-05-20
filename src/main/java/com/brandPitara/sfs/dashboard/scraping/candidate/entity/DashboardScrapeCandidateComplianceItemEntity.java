package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "dashboard_scrape_candidate_compliance_item",
        indexes = @Index(name = "idx_scrape_cand_compliance_cid", columnList = "candidate_id")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateComplianceItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private DashboardScrapeCandidateEntity candidate;

    @Column(name = "item_group", length = 80)
    private String itemGroup;

    @Column(name = "item_key", length = 120)
    private String itemKey;

    @Column(name = "item_label", length = 180)
    private String itemLabel;

    @Column(length = 80)
    private String status;

    @Column(name = "value_text", columnDefinition = "text")
    private String valueText;

    @Column(name = "document_url", columnDefinition = "text")
    private String documentUrl;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean verified;
}
