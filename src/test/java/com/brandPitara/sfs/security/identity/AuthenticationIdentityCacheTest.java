package com.brandPitara.sfs.security.identity;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Ticker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationIdentityCacheTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void sequentialRequestsLoadMobileIdentityOnce() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findAuthenticationSnapshotById(7L)).thenReturn(Optional.of(mobile(7L, true, Role.CUSTOMER)));
        MobileAuthenticationIdentityCache cache = mobileCache(repository, namespace(100, Duration.ofSeconds(30)), Ticker.systemTicker());

        for (int i = 0; i < 100; i++) {
            assertThat(cache.get(7L).userId()).isEqualTo(7L);
        }

        verify(repository).findAuthenticationSnapshotById(7L);
        assertThat(cache.estimatedSize()).isOne();
    }

    @Test
    void concurrentColdRequestsUseOneAtomicLoader() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        CountDownLatch loaderEntered = new CountDownLatch(1);
        CountDownLatch releaseLoader = new CountDownLatch(1);
        when(repository.findAuthenticationSnapshotById(9L)).thenAnswer(invocation -> {
            loaderEntered.countDown();
            assertThat(releaseLoader.await(5, TimeUnit.SECONDS)).isTrue();
            return Optional.of(mobile(9L, true, Role.CUSTOMER));
        });
        MobileAuthenticationIdentityCache cache = mobileCache(repository, namespace(100, Duration.ofSeconds(30)), Ticker.systemTicker());
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MobileAuthenticationUserSnapshot>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            futures.add(executor.submit(() -> {
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return cache.get(9L);
            }));
        }

        start.countDown();
        assertThat(loaderEntered.await(5, TimeUnit.SECONDS)).isTrue();
        releaseLoader.countDown();
        for (Future<MobileAuthenticationUserSnapshot> future : futures) {
            assertThat(future.get(5, TimeUnit.SECONDS).userId()).isEqualTo(9L);
        }
        executor.shutdownNow();

        verify(repository).findAuthenticationSnapshotById(9L);
    }

    @Test
    void expiryReloadsIdentity() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findAuthenticationSnapshotById(4L)).thenReturn(Optional.of(mobile(4L, true, Role.CUSTOMER)));
        MutableTicker ticker = new MutableTicker();
        MobileAuthenticationIdentityCache cache = mobileCache(repository, namespace(100, Duration.ofSeconds(2)), ticker);

        cache.get(4L);
        ticker.advance(Duration.ofSeconds(3));
        cache.get(4L);

        verify(repository, times(2)).findAuthenticationSnapshotById(4L);
    }

    @Test
    void maximumSizeEvictsEntries() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findAuthenticationSnapshotById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.of(mobile(id, true, Role.CUSTOMER));
        });
        MobileAuthenticationIdentityCache cache = mobileCache(repository, namespace(2, Duration.ofMinutes(1)), Ticker.systemTicker());

        cache.get(1L);
        cache.get(2L);
        cache.get(3L);

        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(2);
    }

    @Test
    void disabledConfigurationAlwaysLoadsAndStoresNothing() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findAuthenticationSnapshotById(3L))
                .thenReturn(Optional.of(mobile(3L, true, Role.CUSTOMER)));
        AuthenticationIdentityCacheProperties.Namespace config = namespace(10, Duration.ofMinutes(1));
        config.setEnabled(false);
        MobileAuthenticationIdentityCache cache = mobileCache(repository, config, Ticker.systemTicker());

        cache.get(3L);
        cache.get(3L);

        verify(repository, times(2)).findAuthenticationSnapshotById(3L);
        assertThat(cache.estimatedSize()).isZero();
    }

    @Test
    void missingAndDisabledUsersAreNotCached() {
        UserRepository missingRepository = mock(UserRepository.class);
        when(missingRepository.findAuthenticationSnapshotById(1L)).thenReturn(Optional.empty());
        MobileAuthenticationIdentityCache missing = mobileCache(missingRepository, namespace(10, Duration.ofMinutes(1)), Ticker.systemTicker());

        assertThatThrownBy(() -> missing.get(1L)).isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> missing.get(1L)).isInstanceOf(UsernameNotFoundException.class);
        verify(missingRepository, times(2)).findAuthenticationSnapshotById(1L);

        UserRepository disabledRepository = mock(UserRepository.class);
        when(disabledRepository.findAuthenticationSnapshotById(2L))
                .thenReturn(Optional.of(mobile(2L, false, Role.CUSTOMER)));
        MobileAuthenticationIdentityCache disabled = mobileCache(disabledRepository, namespace(10, Duration.ofMinutes(1)), Ticker.systemTicker());

        assertThatThrownBy(() -> disabled.get(2L)).isInstanceOf(DisabledException.class);
        assertThatThrownBy(() -> disabled.get(2L)).isInstanceOf(DisabledException.class);
        verify(disabledRepository, times(2)).findAuthenticationSnapshotById(2L);
    }

    @Test
    void disabledDashboardUserIsDeniedAfterCacheInvalidation() {
        DashboardUserRepository repository = mock(DashboardUserRepository.class);
        when(repository.findAuthenticationRowsById(22L))
                .thenReturn(List.of(new DashboardAuthenticationUserRow(
                        22L, "writer@example.com", "Writer", DashboardRole.CONTENT_STAFF, true,
                        DashboardPermission.CMS_CONTENT_CREATE
                )))
                .thenReturn(List.of(new DashboardAuthenticationUserRow(
                        22L, "writer@example.com", "Writer", DashboardRole.CONTENT_STAFF, false,
                        DashboardPermission.CMS_CONTENT_CREATE
                )));
        DashboardAuthenticationIdentityCache dashboard = new DashboardAuthenticationIdentityCache(
                repository, namespace(10, Duration.ofMinutes(1)), new SimpleMeterRegistry(), Ticker.systemTicker()
        );
        AuthenticationIdentityCacheInvalidator invalidator = new AuthenticationIdentityCacheInvalidator(
                mock(MobileAuthenticationIdentityCache.class), dashboard
        );

        assertThat(dashboard.get(22L).active()).isTrue();
        invalidator.invalidateDashboardAfterCommit(22L);

        assertThatThrownBy(() -> dashboard.get(22L)).isInstanceOf(DisabledException.class);
        verify(repository, times(2)).findAuthenticationRowsById(22L);
    }

    @Test
    void dashboardPermissionChangesAreVisibleAfterCacheInvalidation() {
        DashboardUserRepository repository = mock(DashboardUserRepository.class);
        when(repository.findAuthenticationRowsById(23L))
                .thenReturn(List.of(new DashboardAuthenticationUserRow(
                        23L, "staff@example.com", "Staff", DashboardRole.CONTENT_STAFF, true,
                        DashboardPermission.CMS_CONTENT_CREATE
                )))
                .thenReturn(List.of(new DashboardAuthenticationUserRow(
                        23L, "staff@example.com", "Staff", DashboardRole.CONTENT_STAFF, true,
                        DashboardPermission.CMS_CONTENT_PUBLISH
                )));
        DashboardAuthenticationIdentityCache dashboard = new DashboardAuthenticationIdentityCache(
                repository, namespace(10, Duration.ofMinutes(1)), new SimpleMeterRegistry(), Ticker.systemTicker()
        );
        AuthenticationIdentityCacheInvalidator invalidator = new AuthenticationIdentityCacheInvalidator(
                mock(MobileAuthenticationIdentityCache.class), dashboard
        );

        assertThat(dashboard.get(23L).permissions())
                .containsExactly(DashboardPermission.CMS_CONTENT_CREATE);
        invalidator.invalidateDashboardAfterCommit(23L);
        assertThat(dashboard.get(23L).permissions())
                .containsExactly(DashboardPermission.CMS_CONTENT_PUBLISH);
    }

    @Test
    void mobileAndDashboardNamespacesAreIsolated() {
        UserRepository mobileRepository = mock(UserRepository.class);
        DashboardUserRepository dashboardRepository = mock(DashboardUserRepository.class);
        when(mobileRepository.findAuthenticationSnapshotById(5L))
                .thenReturn(Optional.of(mobile(5L, true, Role.CUSTOMER)));
        when(dashboardRepository.findAuthenticationRowsById(5L))
                .thenReturn(List.of(new DashboardAuthenticationUserRow(
                        5L, "admin@example.com", "Admin", DashboardRole.ADMIN, true, null
                )));
        AuthenticationIdentityCacheProperties.Namespace config = namespace(10, Duration.ofMinutes(1));
        MobileAuthenticationIdentityCache mobile = mobileCache(mobileRepository, config, Ticker.systemTicker());
        DashboardAuthenticationIdentityCache dashboard = new DashboardAuthenticationIdentityCache(
                dashboardRepository, config, new SimpleMeterRegistry(), Ticker.systemTicker()
        );

        assertThat(mobile.get(5L).role()).isEqualTo(Role.CUSTOMER);
        assertThat(dashboard.get(5L).role()).isEqualTo(DashboardRole.ADMIN);
        mobile.invalidate(5L);

        assertThat(dashboard.get(5L).role()).isEqualTo(DashboardRole.ADMIN);
        verify(dashboardRepository).findAuthenticationRowsById(5L);
    }

    @Test
    void invalidationIsDeferredUntilTransactionCommit() {
        MobileAuthenticationIdentityCache mobile = mock(MobileAuthenticationIdentityCache.class);
        DashboardAuthenticationIdentityCache dashboard = mock(DashboardAuthenticationIdentityCache.class);
        AuthenticationIdentityCacheInvalidator invalidator = new AuthenticationIdentityCacheInvalidator(mobile, dashboard);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        invalidator.invalidateMobileAfterCommit(11L);
        verify(mobile, never()).invalidate(11L);

        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(mobile).invalidate(11L);
    }

    @Test
    void rejectsUnboundedOrLongLivedConfiguration() {
        AuthenticationIdentityCacheProperties properties = new AuthenticationIdentityCacheProperties();
        properties.getMobile().setMaximumSize(0);
        properties.getDashboard().setExpireAfterWrite(Duration.ofHours(1));

        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(properties)).hasSize(2);
    }

    private MobileAuthenticationIdentityCache mobileCache(
            UserRepository repository,
            AuthenticationIdentityCacheProperties.Namespace properties,
            Ticker ticker
    ) {
        return new MobileAuthenticationIdentityCache(repository, properties, new SimpleMeterRegistry(), ticker);
    }

    private AuthenticationIdentityCacheProperties.Namespace namespace(long maximumSize, Duration ttl) {
        AuthenticationIdentityCacheProperties.Namespace namespace = new AuthenticationIdentityCacheProperties.Namespace();
        namespace.setEnabled(true);
        namespace.setMaximumSize(maximumSize);
        namespace.setExpireAfterWrite(ttl);
        return namespace;
    }

    private MobileAuthenticationUserSnapshot mobile(Long id, boolean enabled, Role role) {
        return new MobileAuthenticationUserSnapshot(id, "+919000000000", role, enabled);
    }

    private static final class MutableTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
