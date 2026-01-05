package com.brandPitara.sfs.request.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.request.enums.ServiceRequestInterestStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "service_request_interest",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_sri_req_provider", columnNames = {"service_request_id", "provider_id"})
    },
    indexes = {
        @Index(name = "idx_sri_request", columnList = "service_request_id"),
        @Index(name = "idx_sri_provider", columnList = "provider_id"),
        @Index(name = "idx_sri_status", columnList = "status")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceRequestInterestEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequestEntity request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfileEntity provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceRequestInterestStatus status;

    @Column(columnDefinition = "text")
    private String message; // optional (quote/message later)
}
