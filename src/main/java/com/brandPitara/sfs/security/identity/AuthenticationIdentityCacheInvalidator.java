package com.brandPitara.sfs.security.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class AuthenticationIdentityCacheInvalidator {

    private final MobileAuthenticationIdentityCache mobileCache;
    private final DashboardAuthenticationIdentityCache dashboardCache;

    public void invalidateMobileAfterCommit(Long userId) {
        afterCommit(() -> mobileCache.invalidate(userId));
    }

    public void invalidateDashboardAfterCommit(Long userId) {
        afterCommit(() -> dashboardCache.invalidate(userId));
    }

    private void afterCommit(Runnable invalidation) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidation.run();
                }
            });
            return;
        }
        invalidation.run();
    }
}
