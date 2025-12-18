package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.UserService;
import com.brandPitara.sfs.service.model.UserLoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserLoginResult findOrCreateVerifiedUserByPhone(String phoneNumber) {

        return userRepository.findByPhoneNumber(phoneNumber)
                .map(existing -> {
                    // 🔁 RETURNING USER (re-login)
                    existing.setVerified(true);
                    existing.setLastLoginAt(OffsetDateTime.now());
                    User saved = userRepository.save(existing);
                    return new UserLoginResult(saved, false);  // newUser = false
                })
                .orElseGet(() -> {
                    // 🆕 NEW USER
                    String digitsOnly = phoneNumber.replaceAll("\\D", "");
                    String syntheticEmail = "phone_" + digitsOnly + "@phone.local";

                    User user = new User();
                    user.setEmail(syntheticEmail);
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setPhoneNumber(phoneNumber);
                    user.setVerified(true);
                    user.setRole(Role.CUSTOMER);
                    user.setLastLoginAt(OffsetDateTime.now());
                    // createdAt is set automatically via @CreationTimestamp

                    User saved = userRepository.save(user);
                    return new UserLoginResult(saved, true);   // newUser = true
                });
    }
}
