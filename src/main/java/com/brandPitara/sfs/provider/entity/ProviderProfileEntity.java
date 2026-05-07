package com.brandPitara.sfs.provider.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.provider.enums.ProviderType;
import com.brandPitara.sfs.provider.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "provider_profile",
    indexes = {
        @Index(name = "idx_provider_category_id", columnList = "primary_category_id"),
        @Index(name = "idx_provider_featured",    columnList = "is_featured")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderProfileEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", unique = true)
    private BusinessEntity business;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    private ProviderType providerType;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 180)
    private String headline;

    @Column(columnDefinition = "text")
    private String bio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_category_id", nullable = false)
    private CategoryEntity primaryCategory;

    private Integer experienceYears;

    // brand-only
    private String businessName;
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean featured = false;
}
