package com.brandPitara.sfs.entity;

import java.time.LocalDateTime;

import com.brandPitara.sfs.enums.OtpType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "otps")
/*
 * Legacy/local OTP persistence model. The active production OTP flow uses Twilio
 * Verify, which owns OTP generation, expiry, invalidation, and reuse prevention
 * outside this database. This entity is not used by the current OtpService
 * implementations and should be reviewed in a later cleanup.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String code;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private boolean verified;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
