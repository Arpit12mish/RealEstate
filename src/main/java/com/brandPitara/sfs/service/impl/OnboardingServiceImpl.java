package com.brandPitara.sfs.service.impl;


import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.mapper.OnboardingRoleMapper;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingRole;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.dto.onboarding.SessionResponse;
import com.brandPitara.sfs.service.OnboardingService;
import com.brandPitara.sfs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final UserRepository userRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final OnboardingRoleMapper onboardingRoleMapper;
    
    @Override
    @Transactional
    public SessionResponse chooseRole(Long userId, OnboardingRole onboardingRole) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Optional: prevent changing role after first selection
        // (remove this block if you want to allow updates)
        // if (user.getRoleSelectedAt() != null) {
        //     throw new IllegalStateException("Role already selected");
        // }

        Role role = onboardingRoleMapper.toSystemRole(onboardingRole);

        user.setRole(role);
        user.setRoleSelectedAt(OffsetDateTime.now());

        // onboarding status transition
        if (role == Role.CUSTOMER) {
            user.setOnboardingStatus(OnboardingStatus.CUSTOMER_READY);
        } else if (role == Role.WORKER || role == Role.BRAND || role == Role.BUILDER) {
            user.setOnboardingStatus(OnboardingStatus.PROVIDER_PROFILE_PENDING);
        } else {
            // safety fallback (should never happen because onboardingRole cannot map to ADMIN)
            throw new IllegalArgumentException("Invalid role selection");
        }

        userRepository.save(user);
        return getSession(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var profileOpt = providerProfileRepository.findByUserId(userId);

        // Self-heal onboarding status (production safety)
        OnboardingStatus status = user.getOnboardingStatus();
        if ((user.getRole() == Role.WORKER || user.getRole() == Role.BRAND)) {
            if (profileOpt.isPresent()) {
                if (status != OnboardingStatus.PROVIDER_READY) status = OnboardingStatus.PROVIDER_READY;
            } else {
                if (status != OnboardingStatus.PROVIDER_PROFILE_PENDING) status = OnboardingStatus.PROVIDER_PROFILE_PENDING;
            }
        } else if (user.getRole() == Role.CUSTOMER) {
            if (status == OnboardingStatus.ROLE_PENDING) {
                // user never chose, still pending
            } else if (status != OnboardingStatus.CUSTOMER_READY) {
                status = OnboardingStatus.CUSTOMER_READY;
            }
        }

        String nextAction = switch (status) {
            case ROLE_PENDING -> "CHOOSE_ROLE";
            case CUSTOMER_READY -> "CUSTOMER_HOME";
            case PROVIDER_PROFILE_PENDING -> "PROVIDER_PROFILE_FORM";
            case PROVIDER_READY -> "PROVIDER_HOME";
        };

        Long providerId = profileOpt.map(p -> p.getId()).orElse(null);
        Long businessId = profileOpt.map(p -> p.getBusiness() != null ? p.getBusiness().getId() : null).orElse(null);

        return new SessionResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getRole(),
                status,
                nextAction,
                providerId,
                businessId
        );
    }
}

