package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// import java.util.Collection;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;
        private final UserPhoneLookupService userPhoneLookupService;

        @Override
        public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

                // identifier can be phone OR email (backward compatible). Phone lookups always
                // go through PhoneNumberNormalizer so pre-migration JWT subjects (raw 10-digit,
                // 0-prefixed, 91-prefixed) still resolve to the canonical +91 user.
                User user = userPhoneLookupService.findByPhoneIdentifier(identifier)
                        .or(() -> userRepository.findByEmail(identifier))
                        .orElseThrow(() ->
                                new UsernameNotFoundException("User not found with phone/email: " + identifier));

                return new MobileAuthenticationUserSnapshot(
                        user.getId(),
                        user.getPhoneNumber(),
                        user.getRole(),
                        user.isVerified()
                );
        }
}
