package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes a refresh-token family (reuse detection) in its own,
 * independently-committed transaction.
 *
 * Reuse is always discovered from inside
 * {@link RefreshTokenServiceImpl#rotateRefreshToken}, immediately before
 * that method throws an {@link IllegalArgumentException} to reject the
 * request. That method is class-level {@code @Transactional} with Spring's
 * default rollback rule (rollback on any RuntimeException) — so if the
 * revocation ran in the same transaction, the rollback triggered by the
 * very exception that reports "reuse detected" would silently undo the
 * revocation along with it, leaving the rest of the device's session
 * (e.g. the currently-valid descendant token) untouched. Routing the write
 * through this separate bean's {@code REQUIRES_NEW} transaction makes it
 * commit independently, before control ever returns to the caller that's
 * about to roll back.
 *
 * Must be a separate Spring bean, not a private method on
 * {@code RefreshTokenServiceImpl}: declarative {@code @Transactional} is
 * enforced by a proxy around the bean, so a same-class (self-invoked)
 * private method call bypasses it entirely.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenFamilyRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            refreshTokenRepository.revokeAllByUserId(userId);
            return;
        }
        refreshTokenRepository.revokeActiveByUserIdAndDeviceId(userId, deviceId);
    }
}
