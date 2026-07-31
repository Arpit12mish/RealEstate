package com.brandPitara.sfs.security;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.service.UserPhoneLookupService;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserService {

    private final UserPhoneLookupService userPhoneLookupService;
    private final UserRepository userRepository;

    @Autowired
    public CurrentUserService(UserPhoneLookupService userPhoneLookupService, UserRepository userRepository) {
        this.userPhoneLookupService = userPhoneLookupService;
        this.userRepository = userRepository;
    }

    CurrentUserService(UserPhoneLookupService userPhoneLookupService) {
        this(userPhoneLookupService, null);
    }

    public User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (auth.getPrincipal() instanceof MobileAuthenticationUserSnapshot snapshot) {
            if (userRepository == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User repository unavailable");
            }
            return userRepository.findById(snapshot.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }

        String rawPhone = auth.getName();

        if (rawPhone == null || rawPhone.isBlank() || "anonymousUser".equalsIgnoreCase(rawPhone)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        // Always resolve through PhoneNumberNormalizer (via the shared lookup helper) so
        // pre-migration JWT principals still resolve to the canonical +91 user.
        return userPhoneLookupService.findByPhoneIdentifier(rawPhone)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found for phone: " + rawPhone
                ));
    }

    public Long requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof MobileAuthenticationUserSnapshot snapshot) {
            return snapshot.userId();
        }
        return requireUser().getId();
    }

}
