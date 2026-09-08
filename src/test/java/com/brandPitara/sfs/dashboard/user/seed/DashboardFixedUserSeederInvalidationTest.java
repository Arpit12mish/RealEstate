package com.brandPitara.sfs.dashboard.user.seed;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.security.identity.AuthenticationIdentityCacheInvalidator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardFixedUserSeederInvalidationTest {

    @Test
    void roleEnableAndPasswordSeedUpdateInvalidateDashboardSnapshot() throws Exception {
        DashboardUserRepository repository = mock(DashboardUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthenticationIdentityCacheInvalidator invalidator = mock(AuthenticationIdentityCacheInvalidator.class);
        DashboardUserEntity existing = DashboardUserEntity.builder()
                .id(31L)
                .name("Old")
                .email("admin@example.com")
                .passwordHash("old-hash")
                .role(DashboardRole.REVIEWER)
                .active(false)
                .build();
        when(repository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(encoder.encode("new-password")).thenReturn("new-hash");
        DashboardFixedUserSeeder seeder = new DashboardFixedUserSeeder(repository, encoder, invalidator);
        ReflectionTestUtils.setField(seeder, "seedEnabled", true);
        ReflectionTestUtils.setField(seeder, "updatePasswords", true);
        ReflectionTestUtils.setField(seeder, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(seeder, "adminPassword", "new-password");
        ReflectionTestUtils.setField(seeder, "adminName", "Admin");
        ReflectionTestUtils.setField(seeder, "reviewerEmail", "");
        ReflectionTestUtils.setField(seeder, "dataEntryEmail", "");

        seeder.run();

        verify(repository).save(existing);
        verify(invalidator).invalidateDashboardAfterCommit(31L);
    }
}
