package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Single, shared entry point for resolving a {@link User} from a phone-ish
 * identifier (e.g. a JWT subject / {@code Authentication#getName()} value).
 * Always normalizes through {@link PhoneNumberNormalizer} first so pre- and
 * post-migration phone formats (raw 10-digit, 0-prefixed, 91-prefixed, and
 * canonical +91 form) all resolve to the same user.
 */
@Component
@RequiredArgsConstructor
public class UserPhoneLookupService {

    private final UserRepository userRepository;

    public Optional<User> findByPhoneIdentifier(String identifier) {
        return normalize(identifier)
                .map(PhoneNumberNormalizer::equivalentLookupValues)
                .map(userRepository::findByPhoneNumberIn)
                .flatMap(users -> users.stream().findFirst());
    }

    private Optional<String> normalize(String identifier) {
        try {
            return Optional.of(PhoneNumberNormalizer.normalize(identifier));
        } catch (ResponseStatusException ex) {
            return Optional.empty();
        }
    }
}
