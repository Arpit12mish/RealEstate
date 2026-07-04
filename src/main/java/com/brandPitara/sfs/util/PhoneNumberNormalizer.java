package com.brandPitara.sfs.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }

        String trimmed = phoneNumber.trim().replaceAll("\\s+", "");
        if (trimmed.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }

        String digits = trimmed.replaceAll("\\D", "");

        if (digits.length() == 10 && isValidIndianMobile(digits)) {
            return "+91" + digits;
        }

        if (digits.length() == 11 && digits.startsWith("0") && isValidIndianMobile(digits.substring(1))) {
            return "+91" + digits.substring(1);
        }

        if (digits.length() == 12 && digits.startsWith("91") && isValidIndianMobile(digits.substring(2))) {
            return "+91" + digits.substring(2);
        }

        if (trimmed.matches("^\\+[1-9]\\d{7,14}$")) {
            return trimmed;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Phone number must be a valid E.164 value like +919876543210"
        );
    }

    public static Set<String> equivalentLookupValues(String phoneNumber) {
        String normalized = normalize(phoneNumber);
        Set<String> values = new LinkedHashSet<>();
        values.add(normalized);

        if (normalized.startsWith("+91") && normalized.length() == 13) {
            String national = normalized.substring(3);
            values.add(national);
            values.add("91" + national);
            values.add("0" + national);
        }

        return values;
    }

    private static boolean isValidIndianMobile(String nationalNumber) {
        return nationalNumber != null
                && nationalNumber.matches("^[6-9]\\d{9}$");
    }
}
