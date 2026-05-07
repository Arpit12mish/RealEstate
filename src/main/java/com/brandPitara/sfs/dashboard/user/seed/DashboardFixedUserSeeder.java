package com.brandPitara.sfs.dashboard.user.seed;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardFixedUserSeeder implements CommandLineRunner {

    private final DashboardUserRepository dashboardUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${dashboard.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${dashboard.seed.update-passwords:false}")
    private boolean updatePasswords;

    @Value("${dashboard.seed.admin.email:}")
    private String adminEmail;

    @Value("${dashboard.seed.admin.password:}")
    private String adminPassword;

    @Value("${dashboard.seed.admin.name:SFS Admin}")
    private String adminName;

    @Value("${dashboard.seed.reviewer.email:}")
    private String reviewerEmail;

    @Value("${dashboard.seed.reviewer.password:}")
    private String reviewerPassword;

    @Value("${dashboard.seed.reviewer.name:SFS Reviewer}")
    private String reviewerName;

    @Value("${dashboard.seed.data-entry.email:}")
    private String dataEntryEmail;

    @Value("${dashboard.seed.data-entry.password:}")
    private String dataEntryPassword;

    @Value("${dashboard.seed.data-entry.name:SFS Data Entry}")
    private String dataEntryName;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Dashboard fixed user seeding is disabled.");
            return;
        }

        upsertFixedUser(adminName, adminEmail, adminPassword, DashboardRole.ADMIN);
        upsertFixedUser(reviewerName, reviewerEmail, reviewerPassword, DashboardRole.REVIEWER);
        upsertFixedUser(dataEntryName, dataEntryEmail, dataEntryPassword, DashboardRole.DATA_ENTRY);
    }

    private void upsertFixedUser(
            String name,
            String email,
            String rawPassword,
            DashboardRole role
    ) {
        if (!StringUtils.hasText(email)) {
            log.warn("Skipping dashboard {} seed because email is missing.", role);
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();

        dashboardUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresentOrElse(
                        existingUser -> updateExistingUser(existingUser, name, rawPassword, role),
                        () -> createNewUser(name, normalizedEmail, rawPassword, role)
                );
    }

    private void createNewUser(
            String name,
            String email,
            String rawPassword,
            DashboardRole role
    ) {
        if (!StringUtils.hasText(rawPassword)) {
            log.warn("Skipping dashboard {} user creation for {} because password is missing.", role, email);
            return;
        }

        DashboardUserEntity user = DashboardUserEntity.builder()
                .name(StringUtils.hasText(name) ? name.trim() : role.name())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build();

        dashboardUserRepository.save(user);

        log.info("Created dashboard fixed user: email={}, role={}", email, role);
    }

    private void updateExistingUser(
            DashboardUserEntity user,
            String name,
            String rawPassword,
            DashboardRole role
    ) {
        if (StringUtils.hasText(name)) {
            user.setName(name.trim());
        }

        user.setRole(role);
        user.setActive(true);

        if (updatePasswords && StringUtils.hasText(rawPassword)) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        dashboardUserRepository.save(user);

        log.info("Updated dashboard fixed user: email={}, role={}", user.getEmail(), role);
    }
}