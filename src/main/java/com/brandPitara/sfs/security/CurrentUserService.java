package com.brandPitara.sfs.security;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.UserRepository;
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

    private final UserRepository userRepository;

    public User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String rawPhone = auth.getName();

        if (rawPhone == null || rawPhone.isBlank() || "anonymousUser".equalsIgnoreCase(rawPhone)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        final String phone = normalizePhone(rawPhone);

        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found for phone: " + phone
                ));
    }


    public Long requireUserId() {
        return requireUser().getId();
    }

    private String normalizePhone(String phone) {
        // Example normalization: "+91 98765-43210" -> "9876543210"
        String digits = phone.replaceAll("\\D", "");

        // If you store Indian numbers without country code:
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }

        return digits;
    }
}
