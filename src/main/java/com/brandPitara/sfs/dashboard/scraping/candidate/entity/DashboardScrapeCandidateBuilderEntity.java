package com.brandPitara.sfs.dashboard.scraping.candidate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "dashboard_scrape_candidate_builder",
        indexes = @Index(name = "idx_scrape_cand_builder_cid", columnList = "candidate_id")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardScrapeCandidateBuilderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private DashboardScrapeCandidateEntity candidate;

    @Column(length = 255)
    private String name;

    @Column(length = 80)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(name = "address_line", columnDefinition = "text")
    private String addressLine;

    @Column(name = "city_name", length = 150)
    private String cityName;

    @Column(columnDefinition = "text")
    private String website;
}
