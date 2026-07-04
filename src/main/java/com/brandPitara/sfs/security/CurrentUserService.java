package com.brandPitara.sfs.security;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.service.UserPhoneLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserPhoneLookupService userPhoneLookupService;

    public User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
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
        return requireUser().getId();
    }

}
