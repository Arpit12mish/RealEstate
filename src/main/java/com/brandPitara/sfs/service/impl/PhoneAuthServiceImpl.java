// // PhoneAuthServiceImpl.java
// package com.brandPitara.sfs.service.impl;

// import java.security.SecureRandom;

// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.brandPitara.sfs.dto.AuthResponse;
// import com.brandPitara.sfs.dto.PhoneOtpSendRequest;
// import com.brandPitara.sfs.dto.PhoneOtpVerifyRequest;
// import com.brandPitara.sfs.entity.Otp;
// import com.brandPitara.sfs.entity.User;
// import com.brandPitara.sfs.enums.OtpType;
// import com.brandPitara.sfs.enums.Role;
// import com.brandPitara.sfs.repository.UserRepository;
// import com.brandPitara.sfs.service.OtpService;
// import com.brandPitara.sfs.service.PhoneAuthService;
// import com.brandPitara.sfs.service.SmsService;
// import com.brandPitara.sfs.util.JwtTokenUtil;

// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// @Transactional
// public class PhoneAuthServiceImpl implements PhoneAuthService {

//     private final UserRepository userRepository;
//     private final OtpService otpService;
//     private final SmsService smsService;
//     private final UserDetailsService userDetailsService;
//     private final JwtTokenUtil jwtTokenUtil;
//     private final PasswordEncoder passwordEncoder;

//     @Override
//     public void sendLoginOtp(PhoneOtpSendRequest request) {
//         String phone = normalizePhone(request.getPhoneNumber());

//         // 1. Find or create user by phone
//         User user = userRepository.findByPhoneNumber(phone)
//                 .orElseGet(() -> createPhoneOnlyUser(phone));

//         // 2. Create OTP for PHONE_LOGIN
//         Otp otp = otpService.createOtp(user, OtpType.PHONE_LOGIN);

//         // 3. Send SMS
//         smsService.sendOtpSms(phone, otp.getCode());
//     }

//     @Override
//     public AuthResponse verifyLoginOtp(PhoneOtpVerifyRequest request) {
//         String phone = normalizePhone(request.getPhoneNumber());
//         String code = request.getCode();

//         User user = userRepository.findByPhoneNumber(phone)
//                 .orElseThrow(() -> new RuntimeException("User not found for phone: " + phone));

//         boolean valid = otpService.validateOtp(user, code, OtpType.PHONE_LOGIN);

//         if (!valid) {
//             throw new RuntimeException("Invalid or expired OTP");
//         }

//         // Phone verified == user verified for phone-only flow
//         user.setVerified(true);
//         userRepository.save(user);

//         // Load UserDetails using email (might be synthetic)
//         UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
//         String token = jwtTokenUtil.generateToken(userDetails);

//         return new AuthResponse(
//                 token,
//                 user.getId(),
//                 user.getEmail(),   // can be synthetic like "+91xxxx@phone.local"
//                 user.isVerified()
//         );
//     }

//     private User createPhoneOnlyUser(String phone) {
//         User user = new User();
//         user.setPhoneNumber(phone);
//         user.setRole(Role.ROLE_USER);
//         user.setVerified(false);

//         // ⚠ Synthetic email so that UserDetailsService (email as username) still works
//         String syntheticEmail = phone + "@phone.local";
//         user.setEmail(syntheticEmail);

//         // Random password (not used, but required for UserDetails)
//         String randomPassword = passwordEncoder.encode(generateRandomPassword());
//         user.setPassword(randomPassword);

//         return userRepository.save(user);
//     }

//     private String normalizePhone(String phone) {
//         return phone.trim(); // TODO: convert to E.164 ("+91...") in production
//     }

//     private String generateRandomPassword() {
//         SecureRandom random = new SecureRandom();
//         byte[] bytes = new byte[16];
//         random.nextBytes(bytes);
//         return bytesToHex(bytes);
//     }

//     private String bytesToHex(byte[] bytes) {
//         StringBuilder sb = new StringBuilder();
//         for (byte b : bytes) {
//             sb.append(String.format("%02x", b));
//         }
//         return sb.toString();
//     }
// }
