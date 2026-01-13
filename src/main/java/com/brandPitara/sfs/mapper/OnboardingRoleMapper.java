package com.brandPitara.sfs.mapper;

import org.springframework.stereotype.Component;

import com.brandPitara.sfs.enums.OnboardingRole;
import com.brandPitara.sfs.enums.Role;

@Component
public class OnboardingRoleMapper {
    public Role toSystemRole(OnboardingRole onboardingRole) {
        return switch (onboardingRole) {
            case CUSTOMER -> Role.CUSTOMER;
            case WORKER -> Role.WORKER;
            case BRAND -> Role.BRAND;
            case BUILDER -> Role.BUILDER;
        };
    }
}

