package com.brandPitara.sfs.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "login_history")
@Data
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "login_time", nullable = false)
    private OffsetDateTime loginTime = OffsetDateTime.now();

    private String deviceId;
    private String fcmToken;
    private String ipAddress;
    private String userAgent;

    @Column(nullable = false)
    private String loginType; // OTP or REFRESH

    @Column(nullable = false)
    private boolean success = true;
}
