package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.UserService;
import com.brandPitara.sfs.service.model.UserLoginResult;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserLoginResult findOrCreateVerifiedUserByPhone(String phoneNumber) {
        String normalizedPhone = PhoneNumberNormalizer.normalize(phoneNumber);
        List<User> matchingUsers = userRepository.findByPhoneNumberIn(
                PhoneNumberNormalizer.equivalentLookupValues(normalizedPhone)
        );

        if (matchingUsers.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple accounts found for this phone number. Please contact support."
            );
        }

        if (!matchingUsers.isEmpty()) {
            User existing = matchingUsers.get(0);

            existing.setVerified(true);
            existing.setPhoneNumber(normalizedPhone);
            existing.setLastLoginAt(OffsetDateTime.now());
            User saved = userRepository.save(existing);
            return new UserLoginResult(saved, false);
        }

        String digitsOnly = normalizedPhone.replaceAll("\\D", "");
        String syntheticEmail = "phone_" + digitsOnly + "@phone.local";

        User user = new User();
        user.setEmail(syntheticEmail);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPhoneNumber(normalizedPhone);
        user.setVerified(true);
        user.setRole(Role.CUSTOMER);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);
        user.setLastLoginAt(OffsetDateTime.now());

        User saved = userRepository.save(user);
        return new UserLoginResult(saved, true);
    }
}
