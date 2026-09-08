package com.brandPitara.sfs.dashboard.auth.service.impl;

import com.brandPitara.sfs.dashboard.audit.service.impl.DashboardLoginAuditServiceImpl;
import com.brandPitara.sfs.dashboard.auth.dto.DashboardLoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardAuthTransactionSafetyTest {

    @Test
    void loginAuditJoinsTheOuterTransactionInsteadOfBorrowingASecondConnection() throws Exception {
        Transactional success = DashboardLoginAuditServiceImpl.class
                .getMethod("recordSuccess",
                        com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity.class,
                        HttpServletRequest.class)
                .getAnnotation(Transactional.class);
        Transactional failure = DashboardLoginAuditServiceImpl.class
                .getMethod("recordFailure", String.class, String.class, HttpServletRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(success).isNotNull();
        assertThat(failure).isNotNull();
        assertThat(success.propagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(failure.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    @Test
    void failedLoginCommitsItsJoinedAuditBeforeRethrowingBadCredentials() throws Exception {
        Transactional login = DashboardAuthServiceImpl.class
                .getMethod("login", DashboardLoginRequest.class, HttpServletRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(login).isNotNull();
        assertThat(Arrays.asList(login.noRollbackFor())).contains(BadCredentialsException.class);
    }
}
