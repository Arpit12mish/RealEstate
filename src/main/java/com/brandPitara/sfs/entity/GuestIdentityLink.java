package com.brandPitara.sfs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Append-only record of a guest identity epoch converting to a registered user.
 * Lifecycle code may insert or delete this row for account erasure, but must
 * never reassign it to another user.
 */
@Entity
@Table(name = "guest_identity_link")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestIdentityLink {

    public static final String OTP_CONVERSION = "OTP_CONVERSION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_session_id", nullable = false, unique = true)
    private GuestSession guestSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "installation_id", nullable = false, length = 120)
    private String installationId;

    @Column(name = "linked_at", nullable = false)
    private OffsetDateTime linkedAt;

    @Column(name = "link_type", nullable = false, length = 30)
    private String linkType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
