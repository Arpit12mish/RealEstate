package com.brandPitara.sfs.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
/*
 * Allow-list, not deny-list: only fields explicitly marked @ToString.Include
 * ever appear. A deny-list (@ToString(exclude = ...)) protects only the
 * fields someone remembered to name - any field added later (a token, a new
 * PII column) would default to included. This also sidesteps
 * LazyInitializationException risk from toString() ever touching the lazy
 * "otps" collection on a detached entity.
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(exclude = "otps")
public class User {
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Not included in toString(): direct PII (email, phoneNumber), the
    // password hash, profile photo URLs, and the lazy "otps" relationship.
    @Column(unique = true)
    private String email;

    @Column(name = "name", length = 80)
    private String name;


    @Column
    private String password;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @ToString.Include
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Otp> otps = new ArrayList<>();

    @ToString.Include
        @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;   // first time user row was inserted

    @ToString.Include
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt; // last successful OTP-based login

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 40)
    private OnboardingStatus onboardingStatus = OnboardingStatus.ROLE_PENDING;

    @ToString.Include
    @Column(name = "role_selected_at")
    private OffsetDateTime roleSelectedAt;

    @Column(name = "profile_photo_url", columnDefinition = "text")
    private String profilePhotoUrl;

    @Column(name = "profile_photo_storage_key", length = 500)
    private String profilePhotoStorageKey;
}