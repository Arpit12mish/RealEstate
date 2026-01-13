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

    private String normalizePhone(String input) {
        if (input == null) return null;

        String trimmed = input.trim();
        // keep + if present, remove other non-digits
        String digits = trimmed.replaceAll("[^0-9]", "");

        // Cases:
        // 10 digits -> +91XXXXXXXXXX
        if (digits.length() == 10) return "+91" + digits;

        // 12 digits starting with 91 -> +91XXXXXXXXXX
        if (digits.length() == 12 && digits.startsWith("91")) return "+91" + digits.substring(2);

        // 11 digits starting with 0 -> +91XXXXXXXXXX
        if (digits.length() == 11 && digits.startsWith("0")) return "+91" + digits.substring(1);

        // Already looks like country-coded but no '+'
        if (digits.length() > 10 && digits.startsWith("91")) return "+91" + digits.substring(2);

        // fallback: if original had + and digits look like full number, rebuild
        if (trimmed.startsWith("+")) return "+" + digits;

        // last resort: return as-is digits (but ideally should never reach here)
        return digits;
    }

}
