package com.brandPitara.sfs.mobileupdate.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.EnforcementMode;
import com.brandPitara.sfs.mobileupdate.PolicyState;
import com.brandPitara.sfs.mobileupdate.StoreAvailability;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mobile_app_update_policy")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileAppUpdatePolicyEntity extends BaseEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MobilePlatform platform;

    @Column(name = "latest_version", nullable = false, length = 40)
    private String latestVersion;

    @Column(name = "latest_build", nullable = false)
    private Long latestBuild;

    @Column(name = "minimum_supported_build", nullable = false)
    private Long minimumSupportedBuild;

    @Column(name = "store_url", nullable = false, length = 500)
    private String storeUrl;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "release_notes", columnDefinition = "text")
    private String releaseNotes;

    @Column(name = "remind_after_hours", nullable = false)
    private Integer remindAfterHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_state", nullable = false, length = 16)
    private PolicyState policyState;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_availability", nullable = false, length = 24)
    private StoreAvailability storeAvailability;

    @Column(name = "availability_verified_at")
    private java.time.OffsetDateTime availabilityVerifiedAt;

    @Column(name = "availability_verified_by")
    private Long availabilityVerifiedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "enforcement_mode", nullable = false, length = 32)
    private EnforcementMode enforcementMode;

    @Column(name = "emergency_disabled", nullable = false)
    private Boolean emergencyDisabled;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
