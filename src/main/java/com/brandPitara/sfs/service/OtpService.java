package com.brandPitara.sfs.service;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;
// import java.util.Random;

// import org.springframework.stereotype.Service;

// import com.brandPitara.sfs.entity.Otp;
// import com.brandPitara.sfs.entity.User;
// import com.brandPitara.sfs.enums.OtpType;
// import com.brandPitara.sfs.repository.OtpRepository;

// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// public class OtpService {
//     private static final int OTP_LENGTH = 6;
//     private static final int OTP_EXPIRATION_MINUTES = 5;
    
//     private final OtpRepository otpRepository;
    
//     public String generateOtp() {
//         // Generate a 6-digit numeric OTP
//         Random random = new Random();
//         StringBuilder otp = new StringBuilder();
        
//         for (int i = 0; i < OTP_LENGTH; i++) {
//             otp.append(random.nextInt(10));
//         }
        
//         return otp.toString();
//     }
    
//     public Otp createOtp(User user, OtpType type) {
//         // Clean up expired OTPs for this user
//         cleanUpExpiredOtps(user.getId());
        
//         // Generate and save new OTP
//         String code = generateOtp();
//         LocalDateTime now = LocalDateTime.now();
//         LocalDateTime expiresAt = now.plusMinutes(OTP_EXPIRATION_MINUTES);
        
//         Otp otp = new Otp();
//         otp.setCode(code);
//         otp.setCreatedAt(now);
//         otp.setExpiresAt(expiresAt);
//         otp.setVerified(false);
//         otp.setType(type);
//         otp.setUser(user);
        
//         return otpRepository.save(otp);
//     }
    
//     public boolean validateOtp(User user, String code, OtpType type) {
//         LocalDateTime now = LocalDateTime.now();
        
//         Optional<Otp> otpOptional = otpRepository.findByCodeAndUser_IdAndTypeAndVerifiedIsFalseAndExpiresAtAfter(
//             code, user.getId(), type, now);
        
//         if (otpOptional.isPresent()) {
//             Otp otp = otpOptional.get();
//             otp.setVerified(true);
//             otpRepository.save(otp);
//             return true;
//         }
        
//         return false;
//     }
    
//     private void cleanUpExpiredOtps(Long userId) {
//         LocalDateTime now = LocalDateTime.now();
//         List<Otp> expiredOtps = otpRepository.findByUser_IdAndVerifiedIsFalseAndExpiresAtBefore(userId, now);
//         otpRepository.deleteAll(expiredOtps);
//     }
// }

public interface OtpService {

    void sendOtp(String phoneNumber);           // e.g. +918368046868

    boolean verifyOtp(String phoneNumber, String code);
}