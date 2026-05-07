package com.brandPitara.sfs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "otp_request_tracker",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_otp_tracker_phone", columnNames = "phone_number")
        },
        indexes = {
                @Index(name = "idx_otp_tracker_phone", columnList = "phone_number"),
                @Index(name = "idx_otp_tracker_blocked_until", columnList = "blocked_until"),
                @Index(name = "idx_otp_tracker_cooldown_until", columnList = "cooldown_until")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "last_sent_at")
    private OffsetDateTime lastSentAt;

    @Column(name = "cooldown_until")
    private OffsetDateTime cooldownUntil;

    @Column(name = "send_count_in_window", nullable = false)
    private int sendCountInWindow = 0;

    @Column(name = "send_window_start")
    private OffsetDateTime sendWindowStart;

    @Column(name = "failed_verify_count_in_window", nullable = false)
    private int failedVerifyCountInWindow = 0;

    @Column(name = "verify_window_start")
    private OffsetDateTime verifyWindowStart;

    @Column(name = "blocked_until")
    private OffsetDateTime blockedUntil;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;
}