package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
                

                // identifier can be phone OR email (backward compatible)
                User user = userRepository.findByPhoneNumber(identifier)
                        .or(() -> userRepository.findByEmail(identifier))
                        .orElseThrow(() ->
                                new UsernameNotFoundException("User not found with phone/email: " + identifier));

                // ✅ ALWAYS prefix ROLE_ (Spring standard)
                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                // ✅ principal username = phoneNumber
                return new org.springframework.security.core.userdetails.User(
                        user.getPhoneNumber(),
                        user.getPassword(),
                        user.isVerified(), // enabled
                        true,
                        true,
                        true,
                        authorities
                );
        }
}

