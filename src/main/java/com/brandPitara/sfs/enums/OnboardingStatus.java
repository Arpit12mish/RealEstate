package com.brandPitara.sfs.enums;

public enum OnboardingStatus {
    ROLE_PENDING,               // user logged in but not chosen Customer/Worker/Brand
    CUSTOMER_READY,             // customer can go to home
    PROVIDER_PROFILE_PENDING,   // role chosen WORKER/BRAND but profile form not completed
    PROVIDER_READY              // provider profile completed; can create projects
}
