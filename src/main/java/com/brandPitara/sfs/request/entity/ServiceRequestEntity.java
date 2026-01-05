package com.brandPitara.sfs.request.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.request.enums.ServiceRequestStatus;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "service_request",
        indexes = {
                @Index(name = "idx_sr_status_city", columnList = "status, city_id"),
                @Index(name = "idx_sr_pincode", columnList = "pincode"),
                @Index(name = "idx_sr_customer", columnList = "customer_user_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceRequestEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer who created request
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceRequestStatus status;

    // Location for provider matching
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @Column(length = 120)
    private String locality;

    @Column(length = 12, nullable = false)
    private String pincode;

    @Column(columnDefinition = "text")
    private String notes;

    // Customer selects multiple categories (no subcategory)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "service_request_category",
            joinColumns = @JoinColumn(name = "service_request_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<CategoryEntity> categories = new HashSet<>();
}
