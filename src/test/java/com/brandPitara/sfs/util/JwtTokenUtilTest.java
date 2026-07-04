package com.brandPitara.sfs.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        ReflectionTestUtils.setField(
                jwtTokenUtil,
                "secret",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );
        ReflectionTestUtils.setField(jwtTokenUtil, "expirationMs", 900000L);
    }

    @Test
    void generatedUserTokenUsesNormalizedPhoneAsSubjectAndClaim() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("+919876543210")
                .password("encoded")
                .roles("CUSTOMER")
                .build();

        String token = jwtTokenUtil.generateToken(userDetails, 12L, "+919876543210", "CUSTOMER");
        String phoneClaim = jwtTokenUtil.getClaimFromToken(token, claims -> claims.get("phone", String.class));

        assertThat(jwtTokenUtil.getUsernameFromToken(token)).isEqualTo("+919876543210");
        assertThat(phoneClaim).isEqualTo("+919876543210");
        assertThat(jwtTokenUtil.getUserIdFromToken(token)).isEqualTo(12L);
        assertThat(jwtTokenUtil.getRoleFromToken(token)).isEqualTo("CUSTOMER");
    }
}
